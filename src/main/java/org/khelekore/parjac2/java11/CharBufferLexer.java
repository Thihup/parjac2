package org.khelekore.parjac2.java11;

import java.math.BigInteger;
import java.nio.CharBuffer;
import java.util.BitSet;

import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Lexer;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;

/** A lexer for the java language */
public class CharBufferLexer implements Lexer {
    private final Grammar grammar;
    private final Java11Tokens java11Tokens;
    // We use the position for keeping track of where we are
    private final CharBuffer buf;
    private boolean hasSentEOI = false;

    private int tokenStartPosition = 0;
    private int tokenStartColumn = 0;
    private int currentLine = 1;
    private int currentLineStart = 0;
    private int currentColumn = 0;

    // Text set when we get an lexer ERROR
    private String errorText;

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

    private final int rightShiftId;

    public CharBufferLexer (Grammar grammar, Java11Tokens java11Tokens, CharBuffer buf) {
	this.grammar = grammar;
	this.java11Tokens = java11Tokens;
	this.buf = buf;

	rightShiftId = grammar.getToken (">>").getId ();
    }

    public String getError () {
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

    @Override public ParsePosition getParsePosition () {
	return new ParsePosition (getLineNumber (), getTokenColumn (),
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

    @Override public Token nextToken (BitSet wantedTokens) {
	while (hasMoreTokens ()) {
	    Token t = nextRealToken (wantedTokens);
	    if (java11Tokens.isWhitespace (t) || java11Tokens.isComment (t))
		continue;
	    return t;
	}
	return grammar.END_OF_INPUT;
    }

    private Token nextRealToken (BitSet wantedTokens) {
	tokenStartPosition = buf.position ();
	tokenStartColumn = currentColumn;
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
		return java11Tokens.SUB;

	    // separators
	    case '(':
		return java11Tokens.LEFT_PARENTHESIS;
	    case ')':
		return java11Tokens.RIGHT_PARENTHESIS;
	    case '{':
		return java11Tokens.LEFT_CURLY;
	    case '}':
		return java11Tokens.RIGHT_CURLY;
	    case '[':
		return java11Tokens.LEFT_BRACKET;
	    case ']':
		return java11Tokens.RIGHT_BRACKET;
	    case ';':
		return java11Tokens.SEMICOLON;
	    case ',':
		return java11Tokens.COMMA;
	    case '.':
		return handleDot ();
	    case '@':
		return java11Tokens.AT;
	    case ':':  // : is an operator, :: is a separator
		return handleColon ();

	    // operators (and comments)
	    case '=':
		return handleEquals ();
	    case '>':
		return handleGT (wantedTokens.get (rightShiftId));
	    case '<':
		return handleLT ();
	    case '!':
		return handleExtraEqual (java11Tokens.NOT, java11Tokens.NOT_EQUAL);
	    case '~':
		return java11Tokens.TILDE;
	    case '?':
		return java11Tokens.QUESTIONMARK;
	    case '+':
		return handleDoubleOrEqual (c, java11Tokens.PLUS, java11Tokens.INCREMENT, java11Tokens.PLUS_EQUAL);
	    case '-':
		return handleMinus ();
	    case '*':
		return handleExtraEqual (java11Tokens.MULTIPLY, java11Tokens.MULTIPLY_EQUAL);
	    case '/':
		return handleSlash ();
	    case '%':
		return handleExtraEqual (java11Tokens.REMAINDER, java11Tokens.REMAINDER_EQUAL);
	    case '&':
		return handleDoubleOrEqual (c, java11Tokens.AND, java11Tokens.LOGICAL_AND, java11Tokens.BIT_AND_EQUAL);
	    case '|':
		return handleDoubleOrEqual (c, java11Tokens.OR, java11Tokens.LOGICAL_OR, java11Tokens.BIT_OR_EQUAL);
	    case '^':
		return handleExtraEqual (java11Tokens.XOR, java11Tokens.BIT_XOR_EQUAL);

	    case '\'':
		return readCharacterLiteral ();
	    case '"':
		return readStringLiteral ();

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
		    return readIdentifier (c, wantedTokens);

		errorText = "Illegal character: " + c + "(0x" + Integer.toHexString (c) + ")";
		return grammar.ERROR;
	    }
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
	return java11Tokens.WHITESPACE;
    }

    private Token handleLF () { // easy case
	nextLine ();
	return java11Tokens.LF;
    }

    private Token handleCR () { // might be a CR or CRLF
	Token tt = handleOneExtra (java11Tokens.CR, '\n', java11Tokens.CRLF);
	nextLine ();
	return tt;
    }

    private Token handleDot () {
	Token tt = java11Tokens.DOT;
	if (buf.hasRemaining ()) {
	    buf.mark ();
	    char c2 = nextChar ();
	    if (c2 == '.') {
		if (buf.hasRemaining ()) {
		    char c3 = nextChar ();
		    if (c3 == '.')
			return java11Tokens.ELLIPSIS;
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
	return handleOneExtra (java11Tokens.COLON, ':', java11Tokens.DOUBLE_COLON);
    }

    private Token handleEquals () {
	return handleOneExtra (java11Tokens.EQUAL, '=', java11Tokens.DOUBLE_EQUAL);
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
	Token tt = java11Tokens.MINUS;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '-')
		tt = java11Tokens.DECREMENT;
	    else if (c == '=')
		tt = java11Tokens.MINUS_EQUAL;
	    else if (c == '>')
		tt = java11Tokens.ARROW;
	    else
		pushBack ();
	}
	return tt;
    }

    private Token handleSlash () {
	// /, /=, //, /* ... */
	Token tt = java11Tokens.DIVIDE;
	if (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '=')
		tt = java11Tokens.DIVIDE_EQUAL;
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
	return java11Tokens.END_OF_LINE_COMMENT;
    }

    private Token readOffMultiLineComment () {
	boolean previousWasStar = false;
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (previousWasStar && c == '/')
		return java11Tokens.TRADITIONAL_COMMENT;
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
	return handleLTGT ('<', java11Tokens.LT, java11Tokens.LE,
			   java11Tokens.LEFT_SHIFT, java11Tokens.LEFT_SHIFT_EQUAL);
    }

    private Token handleGT (boolean shiftAllowed) {
	// >, >=, >>, >>=, >>>, >>>=
	Token tt = java11Tokens.GT;

	if (shiftAllowed && buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (c == '=') {
		return java11Tokens.GE;
	    } else if (c == '>') {
		if (buf.hasRemaining ()) {
		    char d = nextChar ();
		    if (d == '=') {
			return java11Tokens.RIGHT_SHIFT_EQUAL;
		    } else if (d == '>') {
			if (buf.hasRemaining ()) {
			    char e = nextChar ();
			    if (e == '=')
				return java11Tokens.RIGHT_SHIFT_UNSIGNED_EQUAL;
			    pushBack ();
			}
		    }
		    pushBack ();
		    return java11Tokens.RIGHT_SHIFT;
		}
	    }
	    pushBack ();
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
	String s =
	    handleString ('\'', java11Tokens.CHARACTER_LITERAL, "Character literal not closed");
	if (s == null)
	    return grammar.ERROR;
	int len = s.length ();
	if (len == 0)
	    return grammar.ERROR;
	if (len > 1) {
	    errorText = "Unclosed character literal: *" + s + "*";
	    return grammar.ERROR;
	}
	currentCharValue = s.charAt (0);
	return java11Tokens.CHARACTER_LITERAL;
    }

    private Token readStringLiteral () {
	String s =
	    handleString ('"', java11Tokens.STRING_LITERAL, "String literal not closed");
	if (s == null)
	    return grammar.ERROR;
	currentStringValue = s;
	return java11Tokens.STRING_LITERAL;
    }

    private String handleString (char end, Token base, String newlineError) {
	errorText = "End of input";

	boolean previousWasBackslash = false;
	StringBuilder res = new StringBuilder ();

	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (previousWasBackslash) {
		switch (c) {
		case 'b': res.append ('\b'); break;
		case 't': res.append ('\t'); break;
		case 'n': res.append ('\n'); break;
		case 'f': res.append ('\f'); break;
		case 'r': res.append ('\r'); break;
		case '"':  // fall through
		case '\'': // fall through
		case '\\': res.append (c); break;

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
		    if (i >= 0 && i < 256)
			res.append ((char)i);
		    else
			errorText = "Invalid octal escape";
		    break;
		default:
		    res.append ('\\');
		    res.append (c);
		    errorText = "Illegal escape sequence";
		}
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
		res.append (c);
	    }
	}
	return errorText == null ? res.toString ().intern () : null;
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
	    if (c == 'x') {
		return readNumber (new StringBuilder (), 16, false);
	    } else if (c == 'b') {
		return readNumber (new StringBuilder (), 2, false);
	    } else if (c == 'l' || c == 'L') {
		currentIntValue = BigInteger.ZERO;
		return java11Tokens.LONG_LITERAL;
	    } else if (c == 'd' || c == 'D') {
		currentDoubleValue = 0.0;
		return java11Tokens.DOUBLE_LITERAL;
	    } else if (c == 'f' || c == 'F') {
		currentDoubleValue = 0.0;
		return java11Tokens.FLOAT_LITERAL;
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
		return java11Tokens.INT_LITERAL;
	    }
	} else {
	    currentIntValue = BigInteger.ZERO;
	}
	return java11Tokens.INT_LITERAL;
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
	Token type = java11Tokens.INT_LITERAL;
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
		type = java11Tokens.DOUBLE_LITERAL;
		break;
	    } else if (c == 'f' || c == 'F') {
		type = java11Tokens.FLOAT_LITERAL;
		break;
	    } else if (c == 'l' || c == 'L') {
		type = java11Tokens.LONG_LITERAL;
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

	if ((hasSeenDot || hasSeenExponent) && type != java11Tokens.FLOAT_LITERAL)
	    type = java11Tokens.DOUBLE_LITERAL;
	if (type == java11Tokens.INT_LITERAL || type == java11Tokens.LONG_LITERAL)
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
	    if (type == java11Tokens.INT_LITERAL)
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
	    if (type == java11Tokens.DOUBLE_LITERAL)
		currentDoubleValue = Double.parseDouble (text);
	    else
		currentFloatValue = Float.parseFloat (text);
	    return type;
	} catch (NumberFormatException n) {
	    errorText = "Failed to parse floating point: " + text;
	    return grammar.ERROR;
	}
    }

    private Token readIdentifier (char start, BitSet wantedTokens) {
	StringBuilder res = new StringBuilder ();
	res.append (start);
	while (buf.hasRemaining ()) {
	    char c = nextChar ();
	    if (Character.isIdentifierIgnorable (c)) {
		// skip it
	    } else if (Character.isJavaIdentifierPart (c)) {
		res.append (c);
	    } else {
		pushBack ();
		break;
	    }
	}
	String identifier = res.toString ();
	Token t = java11Tokens.getKeywordFromIdentifier (identifier);
	if (t != null)
	    return t;
	t = java11Tokens.getRestrictedKeyWordFromIdentifier (identifier);
	if (t != null && wantedTokens.get (t.getId ()))
	    return t;
	currentIdentifier = identifier;
	return java11Tokens.IDENTIFIER;
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
		    }
		}
	    }
	}
	return c;
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
}
