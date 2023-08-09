package org.khelekore.parjac2.javacompiler;

import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.BitSet;
import java.util.Set;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Lexer;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

/** A lexer for the java language */
public class CharBufferLexer implements Lexer {
    private final Grammar grammar;
    private final JavaTokens javaTokens;
    // We use the position for keeping track of where we are
    private final CharBuffer buf;
    private final Path path;
    private final CompilerDiagnosticCollector diagnostics;
    private boolean hasSentEOI = false;

    // TODO: add our own CompilerDiagnosticCollector for lexer errors
    // TODO: return char literal and string literal instead of ERROR, but set value to "" or \0

    private int tokenStartPosition = 0;
    private int tokenStartColumn = 0;
    private int currentLine = 1;
    private int currentLineStart = 0;
    private int currentColumn = 0;

    // Text set when we get an lexer ERROR
    private String errorText;

    private BitSet lastScannedTokens;

    // The different values we can have
    private char currentCharValue;
    private String currentStringValue;
    private BigInteger currentIntValue; // for int and long
    private float currentFloatValue;
    private double currentDoubleValue;
    private String currentIdentifier;

    // To be able to push back
    private int lastCharStart = 0;

    // Decimal values are always positive, hex, oct and binary may be negative
    private static final BigInteger MAX_INT_LITERAL = new BigInteger ("80000000", 16);
    private static final BigInteger MAX_LONG_LITERAL = new BigInteger ("8000000000000000", 16);
    private static final BigInteger MAX_UINT_LITERAL = new BigInteger  ("FFFFFFFF", 16);
    private static final BigInteger MAX_ULONG_LITERAL = new BigInteger ("FFFFFFFFFFFFFFFF", 16);

    private final BitSet multiGTTTokens = new BitSet ();

    // java 11 only has "var"
    // java 17+ has "permits", "record", "sealed", "var", "yield"
    // TODO let constructor or grammar know what to set to use
    private final Set<String> nonTypeIdentifiers = Set.of ("permits", "record", "sealed", "var", "yield");

    public CharBufferLexer (Grammar grammar, JavaTokens javaTokens, CharBuffer buf, Path path,
			    CompilerDiagnosticCollector diagnostics) {
	this.grammar = grammar;
	this.javaTokens = javaTokens;
	this.buf = buf;
	this.path = path;
	this.diagnostics = diagnostics;
	lastScannedTokens = new BitSet (grammar.getNumberOfTokens ());
	multiGTTTokens.set (javaTokens.GE.getId ());
	multiGTTTokens.set (javaTokens.RIGHT_SHIFT_EQUAL.getId ());
	multiGTTTokens.set (javaTokens.RIGHT_SHIFT_UNSIGNED_EQUAL.getId ());
    }

    @Override public String getError () {
	return errorText;
    }

    public char getCharValue () {
	return currentCharValue;
    }

    public String getStringValue () {
	return currentStringValue;
    }

    public int getIntValue () {
	// for 2^31 which is the max allowed int literal we get -2^31.
	// note however that (int)2^31 == (int)(-2^31)
	return currentIntValue.intValue ();
    }

    public long getLongValue () {
	// similar to int handling above
	return currentIntValue.longValue ();
    }

    public float getFloatValue () {
	return currentFloatValue;
    }

    public double getDoubleValue () {
	return currentDoubleValue;
    }

    public String getIdentifier () {
	return currentIdentifier;
    }

    @Override public TokenNode getCurrentValue () {
	if (lastScannedTokens.get (javaTokens.CHARACTER_LITERAL.getId ()))
	    return new CharLiteral (javaTokens.CHARACTER_LITERAL, getCharValue (), getParsePosition ());
	if (lastScannedTokens.get (javaTokens.STRING_LITERAL.getId ()))
	    return new StringLiteral (javaTokens.STRING_LITERAL, getStringValue (), getParsePosition ());
	if (lastScannedTokens.get (javaTokens.INT_LITERAL.getId ()))
	    return new IntLiteral (javaTokens.INT_LITERAL, getIntValue (), getParsePosition ());
	if (lastScannedTokens.get (javaTokens.LONG_LITERAL.getId ()))
	    return new LongLiteral (javaTokens.LONG_LITERAL, getLongValue (), getParsePosition ());
	if (lastScannedTokens.get (javaTokens.FLOAT_LITERAL.getId ()))
	    return new FloatLiteral (javaTokens.FLOAT_LITERAL, getFloatValue (), getParsePosition ());
	if (lastScannedTokens.get (javaTokens.DOUBLE_LITERAL.getId ()))
	    return new DoubleLiteral (javaTokens.DOUBLE_LITERAL, getDoubleValue (), getParsePosition ());
	if (lastScannedTokens.get (javaTokens.IDENTIFIER.getId ()))
	    return new Identifier (javaTokens.IDENTIFIER, getIdentifier (), getParsePosition ());
	Token t = grammar.getToken (lastScannedTokens.nextSetBit (0));
	return new TokenNode (t, getParsePosition ());
    }

    @Override public TokenNode toCorrectType (TokenNode n, Token wantedActualToken) {
	if (wantedActualToken == javaTokens.VAR)
	    return new TokenNode (wantedActualToken, n.getPosition ());
	if (wantedActualToken == javaTokens.TYPE_IDENTIFIER && n instanceof Identifier) {
	    Identifier i = (Identifier)n;
	    if (allowedTypeIdentifier (i))
		return new TypeIdentifier (wantedActualToken, i.getValue (), n.getPosition ());
	}
	return n;
    }

    private boolean allowedTypeIdentifier (Identifier i) {
	return !nonTypeIdentifiers.contains (i.getId ());
    }

    @Override public ParsePosition getParsePosition () {
	return new ParsePosition (getLineNumber (), getTokenColumn (),
				  getTokenStartPos (), getTokenEndPos ());
    }

    /** Used when we find invalid things in strings / chars and we want to report the actual position */
    private ParsePosition currentParsePosition () {
	return new ParsePosition (currentLine, currentColumn,
				  getTokenStartPos (), getTokenEndPos ());
    }

    public int getLineNumber () {
	return currentLine;
    }

    public int getTokenStartPos () {
	return tokenStartPosition;
    }

    public int getTokenEndPos () {
	return buf.position ();
    }

    public int getTokenColumn () {
	return tokenStartColumn;
    }

    @Override public BitSet nextToken (BitSet wantedTokens) {
	while (hasMoreTokens ()) {
	    Token nextToken = nextRealToken (wantedTokens);
	    if (javaTokens.isWhitespace (nextToken) || javaTokens.isComment (nextToken))
		continue;
	    lastScannedTokens.clear ();
	    lastScannedTokens.set (nextToken.getId ());
	    if (nonTypeIdentifiers.contains (nextToken.getName ())) {
		lastScannedTokens.set (javaTokens.IDENTIFIER.getId ());
	    } else if (nextToken == javaTokens.IDENTIFIER &&
		       wantedTokens.get (javaTokens.TYPE_IDENTIFIER.getId ()) &&
		       !nonTypeIdentifiers.contains (currentIdentifier)) {
		lastScannedTokens.set (javaTokens.TYPE_IDENTIFIER.getId ());
	    }
	    return lastScannedTokens;
	}
	lastScannedTokens.clear ();
	lastScannedTokens.set (grammar.END_OF_INPUT.getId ());
	return lastScannedTokens;
    }

    public Token nextRealToken (BitSet wantedTokens) {
	tokenStartPosition = buf.position ();
	tokenStartColumn = currentColumn;
	try {
	    if (buf.hasRemaining ()) {
		char c = nextChar ();
		switch (c) {

		    // whitespace
		case ' ':
		case '\t':
		case '\f':
		    return readWhitespace ();

		    // newlines
		case '\n':
		    return handleLF ();
		case '\r':
		    return handleCR ();

		    // sub
		case '\u001a':
		    return javaTokens.SUB;

		    // separators
		case '(':
		    return javaTokens.LEFT_PARENTHESIS;
		case ')':
		    return javaTokens.RIGHT_PARENTHESIS;
		case '{':
		    return javaTokens.LEFT_CURLY;
		case '}':
		    return javaTokens.RIGHT_CURLY;
		case '[':
		    return javaTokens.LEFT_BRACKET;
		case ']':
		    return javaTokens.RIGHT_BRACKET;
		case ';':
		    return javaTokens.SEMICOLON;
		case ',':
		    return javaTokens.COMMA;
		case '.':
		    return handleDot ();
		case '@':
		    return javaTokens.AT;
		case ':':  // : is an operator, :: is a separator
		    return handleColon ();

		    // operators (and comments)
		case '=':
		    return handleEquals ();
		case '>':
		    return handleGT (wantedTokens.intersects (multiGTTTokens));
		case '<':
		    return handleLT ();
		case '!':
		    return handleExtraEqual (javaTokens.NOT, javaTokens.NOT_EQUAL);
		case '~':
		    return javaTokens.TILDE;
		case '?':
		    return javaTokens.QUESTIONMARK;
		case '+':
		    return handleDoubleOrEqual (c, javaTokens.PLUS, javaTokens.INCREMENT, javaTokens.PLUS_EQUAL);
		case '-':
		    return handleMinus ();
		case '*':
		    return handleExtraEqual (javaTokens.MULTIPLY, javaTokens.MULTIPLY_EQUAL);
		case '/':
		    return handleSlash ();
		case '%':
		    return handleExtraEqual (javaTokens.REMAINDER, javaTokens.REMAINDER_EQUAL);
		case '&':
		    return handleDoubleOrEqual (c, javaTokens.AND, javaTokens.LOGICAL_AND, javaTokens.BIT_AND_EQUAL);
		case '|':
		    return handleDoubleOrEqual (c, javaTokens.OR, javaTokens.LOGICAL_OR, javaTokens.BIT_OR_EQUAL);
		case '^':
		    return handleExtraEqual (javaTokens.XOR, javaTokens.BIT_XOR_EQUAL);

		case '\'':
		    return readCharacterLiteral ();
		case '"':
		    return readTextBlockOrString ();

		case '0':
		    return readZero ();
		case '1':
		case '2':
		case '3':
		case '4':
		case '5':
		case '6':
		case '7':
		case '8':
		case '9':
		    return readDecimalNumber (c);
		default:
		    if (Character.isJavaIdentifierStart (c))
			return readIdentifierOrKeyword (c, wantedTokens);

		    errorText = "Illegal character: " + c + "(0x" + Integer.toHexString (c) + ")";
		    return grammar.ERROR;
		}
	    }
	} catch (InvalidUnicodeEscape e) {
	    errorText = e.getMessage ();
	    return grammar.ERROR;
	}
	hasSentEOI = true;
	return grammar.END_OF_INPUT;
    }

    @Override public boolean hasMoreTokens () {
	return buf.position () != buf.limit () || !hasSentEOI;
    }

    private Token readWhitespace () {
	char c = 0;
	while (buf.hasRemaining ()) {
	    c = nextChar ();
	    if (c != ' ' && c != '\t' && c != '\f') {
		pushBack ();
		break;
	    }
	}
	return javaTokens.WHITESPACE;
    }

    private Token handleLF () { // easy case
	nextLine ();
	return javaTokens.LF;
    }

    private Token handleCR () { // might be a CR or CRLF
	Token tt = handleOneExtra (javaTokens.CR, '\n', javaTokens.CRLF);
	nextLine ();
	return tt;
    }

    private Token handleDot () {
	Token tt = javaTokens.DOT;
	if (buf.hasRemaining ()) {
	    buf.mark ();
	    char c2 = nextChar ();
	    if (c2 == '.') {
		if (buf.hasRemaining ()) {
		    char c3 = nextChar ();
		    if (c3 == '.')
			return javaTokens.ELLIPSIS;
		}
	    } else if (c2 >= '0' && c2 <= '9') {
		StringBuilder value = new StringBuilder ();
		value.append ('.');
		value.append (c2);
		return readNumber (value, 10, true);
	    }
	    buf.reset ();
	}
	return tt;
    }

    private Token handleColon () {
	return handleOneExtra (javaTokens.COLON, ':', javaTokens.DOUBLE_COLON);
    }

    private Token handleEquals () {
	return handleOneExtra (javaTokens.EQUAL, '=', javaTokens.DOUBLE_EQUAL);
    }

    private Token handleExtraEqual (Token base, Token extra) {
	return handleOneExtra (base, '=', extra);
    }

    private Token handleDoubleOrEqual (char m, Token base, Token twice, Token baseEqual) {
	Token tt = base;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == m)
		tt = twice;
	    else if (c == '=')
		tt = baseEqual;
	    else
		pushBack ();
	}
	return tt;
    }

    private Token handleMinus () {
	// -, --, -=, ->
	Token tt = javaTokens.MINUS;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '-')
		tt = javaTokens.DECREMENT;
	    else if (c == '=')
		tt = javaTokens.MINUS_EQUAL;
	    else if (c == '>')
		tt = javaTokens.ARROW;
	    else
		pushBack ();
	}
	return tt;
    }

    private Token handleSlash () {
	// /, /=, //, /* ... */
	Token tt = javaTokens.DIVIDE;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '=')
		tt = javaTokens.DIVIDE_EQUAL;
	    else if (c == '/')
		tt = readOffOneLineComment ();
	    else if (c == '*')
		tt = readOffMultiLineComment ();
	    else
		pushBack ();
	}
	return tt;
    }

    private Token readOffOneLineComment () {
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '\n' || c == '\r') {
		pushBack ();
		break;
	    }
	}
	return javaTokens.END_OF_LINE_COMMENT;
    }

    private Token readOffMultiLineComment () {
	boolean previousWasStar = false;
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (previousWasStar && c == '/')
		return javaTokens.TRADITIONAL_COMMENT;
	    previousWasStar = (c == '*');
	    if (c == '\n')
		handleLF ();
	    else if (c == '\r')
		handleCR ();
	}
	errorText = "Reached end of input while inside comment";
	return grammar.ERROR;
    }

    private Token handleLT () {
	// <, <=, <<, <<=
	return handleLTGT ('<', javaTokens.LT, javaTokens.LE,
			   javaTokens.LEFT_SHIFT, javaTokens.LEFT_SHIFT_EQUAL);
    }

    private Token handleGT (boolean multiCharsAllowed) {
	// We would like to handle: >, >=, >>, >>=, >>>, >>>=
	// but due to grammar being conflicting we only do
	// > and >=, >>= and >>>=
	Token tt = javaTokens.GT;

	if (multiCharsAllowed && buf.hasRemaining ()) {
	    // Save state for column and char start
	    int cc = currentColumn;
	    char c = nextChar ();
	    int lcs = lastCharStart;
	    if (c == '=') {
		return javaTokens.GE;
	    } else if (c == '>') {
		if (buf.hasRemaining ()) {
		    char d = nextChar ();
		    if (d == '=') {
			return javaTokens.RIGHT_SHIFT_EQUAL;
		    } else if (d == '>') {
			if (buf.hasRemaining ()) {
			    char e = nextChar ();
			    if (e == '=')
				return javaTokens.RIGHT_SHIFT_UNSIGNED_EQUAL;
			}
		    }
		}
	    }
	    // We failed to scan a multi char token, restore state
	    buf.position (lcs);
	    currentColumn = cc;
	}
	return tt;
    }

    private Token handleLTGT (char ltgt, Token base, Token baseEqual,
			      Token doubleBase, Token doubleBaseEqual) {
	Token tt = base;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '=')
		tt = baseEqual;
	    else if (c == ltgt)
		tt = handleOneExtra (doubleBase, '=', doubleBaseEqual);
	    else
		pushBack ();
	}
	return tt;
    }

    private Token handleOneExtra (Token base, char match, Token extended) {
	Token tt = base;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == match)
		tt = extended;
	    else // push back what we read
		pushBack ();
	}
	return tt;
    }

    private Token readCharacterLiteral () {
	currentCharValue = '\0';
	String s =
	    handleString ('\'', javaTokens.CHARACTER_LITERAL, "Character literal not closed");
	if (s == null) {
	    errorText = null; // already reported inside handleString
	} else {
	    int len = s.length ();
	    if (len == 0) {
		errorText = "Empty character literal";
	    } else if (len > 1) {
		errorText = "Unclosed character literal: *" + s + "*";
	    } else {
		currentCharValue = s.charAt (0);
	    }
	}
	if (errorText != null)
	    diagnostics.report (SourceDiagnostics.error (path, currentParsePosition (), errorText));
	return javaTokens.CHARACTER_LITERAL;
    }

    private Token readTextBlockOrString () {
	// we have seen one '"'
	if (peek ('"', '"')) {
	    buf.position (buf.position () + 2);
	    return readTextBlock ();
	}
	return readStringLiteral ();
    }

    private Token readTextBlock () {
	return textReturn (handleTextBlock ());
    }

    private String handleTextBlock () {
	errorText = "End of input";
	boolean previousWasBackslash = false;
	StringBuilder sb = new StringBuilder ();
	int seenQuotes = 0;

	while (buf.hasRemaining ()) {
	    char c = nextTextChar (previousWasBackslash);
	    if (previousWasBackslash) {
		addEscapedChar (sb, c);
		seenQuotes = 0;
		previousWasBackslash = false;
	    } else if (c == '"') {
		seenQuotes++;
		if (seenQuotes == 3) {
		    sb.setLength (sb.length () - 2); // remove the two quotes
		    errorText = null;
		    break;
		}
		sb.append (c);
	    } else if (c == '\\') {
		seenQuotes = 0;
		previousWasBackslash = true;
	    } else {
		if (c == '\n' || (c == '\r' && !peek ('\n')))
		    nextLine ();
		seenQuotes = 0;
		sb.append (c);
	    }
	}
	if (errorText != null)
	    return null;

	String s = sb.toString ();
	char c = s.charAt (0);
	if (c == ' ' || c == '\t' || c == '\f')
	    s = s.replaceFirst ("^[ \t\f]*(\r\n?|\n)", "");
	s = s.stripIndent ();
	return s;
    }

    private Token readStringLiteral () {
	return textReturn (handleString ('"', javaTokens.STRING_LITERAL,
					 "String literal not closed"));
    }

    private Token textReturn (String s) {
	if (s == null)
	    return grammar.ERROR;
	currentStringValue = s;
	return javaTokens.STRING_LITERAL;
    }

    private String handleString (char end, Token base, String newlineError) {
	errorText = "End of input";

	boolean previousWasBackslash = false;
	StringBuilder sb = new StringBuilder ();

	while (buf.hasRemaining ()) {
	    char c = nextTextChar (previousWasBackslash);
	    if (previousWasBackslash) {
		addEscapedChar (sb, c);
		previousWasBackslash = false;
	    } else if (c == '\n' || c == '\r') {
		errorText = newlineError;
		pushBack ();
	    } else if (c == end) {
		errorText = null;
		break;
	    } else if (c == '\\') {
		previousWasBackslash = true;
	    } else {
		sb.append (c);
	    }
	}
	if (errorText != null)
	    diagnostics.report (SourceDiagnostics.error (path, currentParsePosition (), errorText));
	return errorText == null ? sb.toString ().intern () : null;
    }

    private char nextTextChar (boolean previousWasBackslash) {
	if (previousWasBackslash)
	    return nextNonUnicodeChar ();
	return nextChar ();
    }

    private void addEscapedChar (StringBuilder sb, char c) {
	switch (c) {
	case 'b': sb.append ('\b'); break;
	case 't': sb.append ('\t'); break;
	case 'n': sb.append ('\n'); break;
	case 'f': sb.append ('\f'); break;
	case 'r': sb.append ('\r'); break;
	case '"':  // fall through
	case '\'': // fall through
	case '\\': sb.append (c); break;

	    // octal escape, Unicode \u0000 to \u00ff
	case '0':
	case '1':
	case '2':
	case '3':
	case '4':
	case '5':
	case '6':
	case '7':
	    int i = getOctalEscape (c);
	    if (i >= 0 && i < 256) {
		sb.append ((char)i);
	    } else {
		sb.append ((char)0);
		errorText = "Invalid octal escape: " + Integer.toOctalString (i);
		diagnostics.report (SourceDiagnostics.error (path, currentParsePosition (), errorText));
	    }
	    break;
	default:
	    sb.append ((char)0);
	    errorText = "Illegal escape sequence";
	    diagnostics.report (SourceDiagnostics.error (path, currentParsePosition (), errorText));
	}
    }

    private int getOctalEscape (char start) {
	StringBuilder sb = new StringBuilder ();
	sb.append (start);
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c >= '0' && c <= '7') {
		sb.append (c);
	    } else {
		pushBack ();
		break;
	    }
	}
	try {
	    return Integer.parseInt (sb.toString (), 8);
	} catch (NumberFormatException n) {
	    return -1;
	}
    }

    private Token readZero () {
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    c = Character.toLowerCase (c);
	    if (c == 'x') {
		return readNumber (new StringBuilder (), 16, false);
	    } else if (c == 'b') {
		return readNumber (new StringBuilder (), 2, false);
	    } else if (c == 'l') {
		currentIntValue = BigInteger.ZERO;
		return javaTokens.LONG_LITERAL;
	    } else if (c == 'd') {
		currentDoubleValue = 0.0;
		return javaTokens.DOUBLE_LITERAL;
	    } else if (c == 'f') {
		currentDoubleValue = 0.0;
		return javaTokens.FLOAT_LITERAL;
	    } else if (c == '_' || (c >= '0' && c <= '7')) {
		StringBuilder value = new StringBuilder ();
		value.append (c);
		return readNumber (value, 8, false);
	    } else if (c == '.') {
		StringBuilder value = new StringBuilder ();
		value.append ('0');
		value.append (c);
		return readNumber (value, 10, true);
	    } else {
		currentIntValue = BigInteger.ZERO;
		pushBack ();
		return javaTokens.INT_LITERAL;
	    }
	} else {
	    currentIntValue = BigInteger.ZERO;
	}
	return javaTokens.INT_LITERAL;
    }

    private Token readDecimalNumber (char start) {
	StringBuilder res = new StringBuilder ();
	res.append (start);
	Token t = readNumber (res, 10, false);
	if (t == grammar.ERROR)
	    return t;

	return t;
    }

    private Token readNumber (StringBuilder value, int radix, boolean hasSeenDot) {
	boolean lastWasUnderscore = false;
	boolean hasSeenExponent = false;
	Token type = javaTokens.INT_LITERAL;
	char minChar = '0';
	char maxChar = (char)(minChar + Math.min (10, radix));
	while (buf.hasRemaining ()) {
	    lastWasUnderscore = false;
	    char c = nextChar ();
	    if (c >= minChar && c < maxChar) {
		value.append (c);
	    } else if (isAllowedHexDigit (radix, hasSeenExponent, c)) {
		value.append (c);
	    } else if (c == '_') { // skip it
		lastWasUnderscore = true;
	    } else if (c == 'd' || c == 'D') {
		type = javaTokens.DOUBLE_LITERAL;
		break;
	    } else if (c == 'f' || c == 'F') {
		type = javaTokens.FLOAT_LITERAL;
		break;
	    } else if (c == 'l' || c == 'L') {
		type = javaTokens.LONG_LITERAL;
		break;
	    } else if (c == '.' && !hasSeenDot && (radix == 10 || radix == 16)) {
		hasSeenDot = true;
		value.append (c);
	    } else if (validExponent (radix, hasSeenExponent, c)) {
		hasSeenExponent = true;
		value.append (c);
		if (!readSignedInteger (value))
		    return grammar.ERROR;
	    } else {
		pushBack ();
		break;
	    }
	}
	if (lastWasUnderscore) {
	    errorText = "Number may not end with underscore";
	    return grammar.ERROR;
	}
	if (value.length () == 0) {
	    errorText = "Number may not be empty";
	    return grammar.ERROR;
	}

	if ((hasSeenDot || hasSeenExponent) && type != javaTokens.FLOAT_LITERAL)
	    type = javaTokens.DOUBLE_LITERAL;
	if (type == javaTokens.INT_LITERAL || type == javaTokens.LONG_LITERAL)
	    return intValue (value.toString (), radix, type);
	return doubleValue (value.toString (), radix, type);
    }

    private boolean isAllowedHexDigit (int radix, boolean hasSeenExponent, char c) {
	if (radix != 16)
	    return false;
	// Exponents are decimal only
	if (hasSeenExponent)
	    return false;
	return (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }

    private boolean validExponent (int radix, boolean hasSeenExponent, char c) {
	if (hasSeenExponent)
	    return false;
	if (radix == 10 && (c == 'e' || c == 'E'))
	    return true;
	if (radix == 16 && (c == 'p' || c == 'P'))
	    return true;
	return false;
    }

    private boolean readSignedInteger (StringBuilder value) {
	boolean lastWasUnderscore = false;
	boolean first = true;
	boolean foundDigits = false;
	while (buf.hasRemaining ()) {
	    lastWasUnderscore = false;
	    char c = nextChar ();
	    if (c >= '0' && c <= '9') {
		value.append (c);
		foundDigits = true;
	    } else if (c == '_') {
		lastWasUnderscore = true;
	    } else if (first && (c == '+' || c == '-')) {
		value.append (c);
	    } else {
		pushBack ();
		break;
	    }
	}
	if (lastWasUnderscore) {
	    errorText = "Number may not end with underscore";
	    return false;
	}
	if (!foundDigits) {
	    errorText = "Exponent not found";
	    return false;
	}
	return true;
    }

    private Token intValue (String text, int radix, Token type) {
	try {
	    currentIntValue = new BigInteger (text, radix);
	    BigInteger maxAllowed;
	    if (type == javaTokens.INT_LITERAL)
		maxAllowed = radix == 10 ? MAX_INT_LITERAL : MAX_UINT_LITERAL;
	    else
		maxAllowed = radix == 10 ? MAX_LONG_LITERAL : MAX_ULONG_LITERAL;

	    if (currentIntValue.compareTo (maxAllowed) > 0) {
		errorText = "Integer literal too large";
		return grammar.ERROR;
	    }
	    return type;
	} catch (NumberFormatException n) {
	    errorText = "Failed to parse int value: " + text;
	    return grammar.ERROR;
	}
    }

    private Token doubleValue (String text, double radix, Token type) {
	if (radix == 16)
	    text = "0x" + text;
	try {
	    if (type == javaTokens.DOUBLE_LITERAL)
		currentDoubleValue = Double.parseDouble (text);
	    else
		currentFloatValue = Float.parseFloat (text);
	    return type;
	} catch (NumberFormatException n) {
	    errorText = "Failed to parse floating point: " + text;
	    return grammar.ERROR;
	}
    }

    private Token readIdentifierOrKeyword (char start, BitSet wantedTokens) {
	StringBuilder res = new StringBuilder ();
	res.append (start);
	boolean tryNonSealed = wantedTokens.get (javaTokens.NON_SEALED.getId ());
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (Character.isIdentifierIgnorable (c)) {
		// skip it
	    } else if (Character.isJavaIdentifierPart (c)) {
		res.append (c);
	    } else {
		if (tryNonSealed && c == '-' && res.toString ().equals ("non")) {
		    int cc = currentColumn;
		    int lcs = lastCharStart;
		    char nc = nextChar ();
		    if (nc == 's') {
			Token t = readIdentifierOrKeyword (nc, wantedTokens);
			if (t == javaTokens.IDENTIFIER && currentIdentifier.equals ("sealed"))
			    currentIdentifier = "non-sealed";
			return javaTokens.NON_SEALED;
		    }
		    buf.position (lcs);
		    currentColumn = cc;
		    break;
		}
		pushBack ();
		break;
	    }
	}
	String identifier = res.toString ().intern ();
	Token t = javaTokens.getKeywordFromIdentifier (identifier);
	if (t != null) {
	    currentIdentifier = t.getName ();
	    return t;
	}
	t = javaTokens.getContextualKeyWordFromIdentifier (identifier);
	if (t != null && wantedTokens.get (t.getId ())) {
	    currentIdentifier = t.getName (); 
	    return t;
	}
	currentIdentifier = identifier;
	return javaTokens.IDENTIFIER;
    }

    private char nextChar () {
	lastCharStart = buf.position ();
	currentColumn++;
	char c = buf.get ();
	if (c == '\\') {
	    // check for unicode escapes \u1234
	    if (buf.remaining () > 4) {
		int p = buf.position ();
		if (buf.get (p++) == 'u') {
		    StringBuilder sb = new StringBuilder ();
		    for (int j = 0; j < 4; j++) {
			char h = buf.get (p++);
			if ((h >= '0' && h <= '9') || (h >= 'a' && h <= 'f') || (h >= 'A' && h <= 'F'))
			    sb.append (h);
			else
			    break;
		    }
		    if (sb.length () == 4) {
			int hv = Integer.parseInt (sb.toString (), 16);
			buf.position (p);
			currentColumn += 4;
			c = (char)hv;
		    } else {
			throw new InvalidUnicodeEscape ("Unicode escape sequence requires 4 hexadecimal " +
							"digits, got: " + sb);
		    }
		}
	    }
	}
	return c;
    }

    private char nextNonUnicodeChar () {
	lastCharStart = buf.position ();
	currentColumn++;
	char c = buf.get ();
	return c;
    }

    /** Peek at the coming character, will not handle unicode escapes and will not update the buffer position.
     */
    private boolean peek (char c) {
	if (buf.remaining () < 1)
	    return false;
	return buf.get (buf.position ()) == c;
    }

    /** Peek at the coming two characters, will not handle unicode escapes and will not update the buffer position.
     */
    private boolean peek (char c1, char c2) {
	if (buf.remaining () < 2)
	    return false;
	int p = buf.position ();
	return buf.get (p++) == c1 && buf.get (p) == c2;
    }

    private void pushBack () {
	int charLength = buf.position () - lastCharStart;
	currentColumn -= charLength;
	buf.position (buf.position () - charLength);
    }

    private void nextLine () {
	currentLine++;
	currentColumn = 0;
	currentLineStart = buf.position ();
    }

    public String getCurrentLine () {
	int p = currentLineStart;
	CharBuffer cb = buf.duplicate ();
	cb.position (p);
	int max = buf.limit ();
	char c;
	while (p < max && (c = buf.get (p)) != '\n' && c != '\r')
	    p++;
	cb.limit (p);
	return cb.toString ();
    }

    private class InvalidUnicodeEscape extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public InvalidUnicodeEscape (String msg) {
	    super (msg);
	}
    }
}
