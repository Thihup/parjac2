package org.khelekore.parjac2.javacompiler;

import java.nio.CharBuffer;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.BitSet;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Token;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestLexer {

    private final Grammar grammar = new Grammar ();
    private final JavaTokens javaTokens = new JavaTokens (grammar);

    CompilerDiagnosticCollector diagnostics;

    @BeforeTest
    public void setupDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSpaceWhitespace () {
	testInput (" ", javaTokens.WHITESPACE);
	testInput ("    ", javaTokens.WHITESPACE);
	testInput ("\t", javaTokens.WHITESPACE);
	testInput ("\f", javaTokens.WHITESPACE);
	testInput ("\t\t\t    ", javaTokens.WHITESPACE);
    }

    @Test
    public void testNewlines () {
	testInput ("\n", javaTokens.LF);
	testInput ("\r", javaTokens.CR);
	testInput ("\r \n", javaTokens.CR, javaTokens.WHITESPACE, javaTokens.LF);
	testInput ("\r\n", javaTokens.CRLF);
	testInput ("\n\r", javaTokens.LF, javaTokens.CR);
    }

    @Test
    public void testSub () {
	testInput ("\u001a", javaTokens.SUB);
    }

    @Test
    public void testColon () {
	testInput (":", javaTokens.COLON);
	testInput (": :", javaTokens.COLON, javaTokens.WHITESPACE, javaTokens.COLON);
	testInput ("::", javaTokens.DOUBLE_COLON);
	testInput (":::", javaTokens.DOUBLE_COLON, javaTokens.COLON);
	testInput ("::::", javaTokens.DOUBLE_COLON, javaTokens.DOUBLE_COLON);
	testInput (":: ::", javaTokens.DOUBLE_COLON, javaTokens.WHITESPACE, javaTokens.DOUBLE_COLON);
    }

    @Test
    public void testDot () {
	testInput (".", javaTokens.DOT);
	testInput ("..", javaTokens.DOT, javaTokens.DOT);
	testInput ("...", javaTokens.ELLIPSIS);
    }

    @Test
    public void testSeparators () {
	testInput ("(", javaTokens.LEFT_PARENTHESIS);
	testInput (")", javaTokens.RIGHT_PARENTHESIS);
	testInput ("{", javaTokens.LEFT_CURLY);
	testInput ("}", javaTokens.RIGHT_CURLY);
	testInput ("[", javaTokens.LEFT_BRACKET);
	testInput ("]", javaTokens.RIGHT_BRACKET);
	testInput (";", javaTokens.SEMICOLON);
	testInput (",", javaTokens.COMMA);
	testInput ("@", javaTokens.AT);
    }

    @Test
    public void testOperators () {
	BitSet wantedTokens = new BitSet ();
	wantedTokens.set (javaTokens.GE.getId ()); // we need to signal that we want >= and similar
	testInput ("=", wantedTokens, javaTokens.EQUAL);
	testInput ("= =", wantedTokens, javaTokens.EQUAL, javaTokens.WHITESPACE, javaTokens.EQUAL);
	testInput (">", wantedTokens, javaTokens.GT);
	testInput ("<", wantedTokens, javaTokens.LT);
	testInput ("!", wantedTokens, javaTokens.NOT);
	testInput ("~", wantedTokens, javaTokens.TILDE);
	testInput ("?", wantedTokens, javaTokens.QUESTIONMARK);
	testInput (":", wantedTokens, javaTokens.COLON);
	testInput ("->", wantedTokens, javaTokens.ARROW);
	testInput ("==", wantedTokens, javaTokens.DOUBLE_EQUAL);
	testInput (">=", wantedTokens, javaTokens.GE); // Not sure about this one
	testInput ("<=", wantedTokens, javaTokens.LE);
	testInput ("!=", wantedTokens, javaTokens.NOT_EQUAL);
	testInput ("&&", wantedTokens, javaTokens.LOGICAL_AND);
	testInput ("||", wantedTokens, javaTokens.LOGICAL_OR);
	testInput ("++", wantedTokens, javaTokens.INCREMENT);
	testInput ("--", wantedTokens, javaTokens.DECREMENT);
	testInput ("+", wantedTokens, javaTokens.PLUS);
	testInput ("-", wantedTokens, javaTokens.MINUS);
	testInput ("*", wantedTokens, javaTokens.MULTIPLY);
	testInput ("/", wantedTokens, javaTokens.DIVIDE);
	testInput ("&", wantedTokens, javaTokens.AND);
	testInput ("|", wantedTokens, javaTokens.OR);
	testInput ("^", wantedTokens, javaTokens.XOR);
	testInput ("%", wantedTokens, javaTokens.REMAINDER);
	testInput ("<<", wantedTokens, javaTokens.LEFT_SHIFT);
	testInput (">>", wantedTokens, javaTokens.GT, javaTokens.GT);
	testInput (">>>", wantedTokens, javaTokens.GT, javaTokens.GT, javaTokens.GT);
	testInput ("+=", wantedTokens, javaTokens.PLUS_EQUAL);
	testInput ("-=", wantedTokens, javaTokens.MINUS_EQUAL);
	testInput ("*=", wantedTokens, javaTokens.MULTIPLY_EQUAL);
	testInput ("/=", wantedTokens, javaTokens.DIVIDE_EQUAL);
	testInput ("&=", wantedTokens, javaTokens.BIT_AND_EQUAL);
	testInput ("|=", wantedTokens, javaTokens.BIT_OR_EQUAL);
	testInput ("^=", wantedTokens, javaTokens.BIT_XOR_EQUAL);
	testInput ("%=", wantedTokens, javaTokens.REMAINDER_EQUAL);
	testInput ("<<=", wantedTokens, javaTokens.LEFT_SHIFT_EQUAL);
	testInput (">>=", wantedTokens, javaTokens.RIGHT_SHIFT_EQUAL);
	testInput (">>>=", wantedTokens, javaTokens.RIGHT_SHIFT_UNSIGNED_EQUAL);
    }

    @Test
    public void testGenerics () {
	// Now we do not signal that we want multi-character >-thingies
	testInput (">>", javaTokens.GT, javaTokens.GT);
    }

    @Test
    public void testComment () {
	testInput ("// whatever", javaTokens.END_OF_LINE_COMMENT);
	testInput ("/* whatever */", javaTokens.TRADITIONAL_COMMENT);
	testInput ("/* this comment /* // /** ends here: */", javaTokens.TRADITIONAL_COMMENT);
	testInput ("/* whatever", grammar.ERROR);
	testInput ("/* whatever \n whatever */", javaTokens.TRADITIONAL_COMMENT);
	testInput ("/* whatever \n * whatever \n *\n/*/", javaTokens.TRADITIONAL_COMMENT);
    }

    @Test
    public void testCharLiteral () {
	testChar ("'a'", 'a');
	testChar ("'\\\\'", '\\');
	testChar ("'\\t'", '\t');
	testInput ("'\\1'", javaTokens.CHARACTER_LITERAL);
	testInput ("'\\12'", javaTokens.CHARACTER_LITERAL);
	testInput ("'\\123'", javaTokens.CHARACTER_LITERAL);

	// test bad input
	testBadChar ("''");
	testBadChar ("'    '");
	testBadChar ("'");
	testBadChar ("'     ");
	testBadChar ("'\\456'");
	testBadChar ("'\\a'");
	testBadChar ("'ab'");
	testBadChar ("'a");
    }

    private void testChar (String toLex, char value) {
	CharBufferLexer l = getLexer (toLex);
	testLexing (l, new BitSet (), javaTokens.CHARACTER_LITERAL);
	char res = l.getCharValue ();
	assert value == res : "Wrong string value: " + res;
    }

    private void testBadChar (String toLex) {
	int errorsBefore = diagnostics.errorCount ();
	testInput (toLex, javaTokens.CHARACTER_LITERAL);
	int errorsAfter =  diagnostics.errorCount ();
	int wanted = errorsBefore + 1;
	assert errorsAfter == wanted : "Did not see wanted errors: wanted: " + wanted + ", got: " + errorsAfter;
    }

    @Test
    public void testStringLiteral () {
	testString ("\"\"", "");
	testString ("\"abc123\"", "abc123");
	testString ("\"\\r\\n\"", "\r\n");
	testBadString ("\"\\q\"");
    }

    @Test
    public void testUnicodeEscape () {
	testInput ("\u0009", javaTokens.WHITESPACE);

	// "\\" is one quoted \, the rest are normal characters
	// https://docs.oracle.com/javase/specs/jls/se20/html/jls-3.html#jls-3.2
	testString ("\"\\\\u1234\"", "\\u1234");

	// 5c = '\', 5a = 'Z', a unicode escape does not start futher unicode esacpes
	// https://mail.openjdk.org/pipermail/compiler-dev/2021-June/017347.html
	// but \u005cu005a can not be put in a string literal so not sure how to test it
	//testString ("\"\\u005cu005a\"", "\\u005a");
	testInput ("\\u000a", javaTokens.LF);
    }

    private void testString (String toLex, String value) {
	CharBufferLexer l = getLexer (toLex);
	testLexing (l, new BitSet (), javaTokens.STRING_LITERAL);
	String res = l.getStringValue ();
	assert res.equals (value) : "Wrong string value: " + res;
    }

    private void testBadString (String toLex) {
	CharBufferLexer l = getLexer (toLex);
	int errorsBefore = diagnostics.errorCount ();
	testLexing (l, new BitSet (), javaTokens.STRING_LITERAL);
	int errorsAfter =  diagnostics.errorCount ();
	int wanted = errorsBefore + 1;
	assert errorsAfter == wanted : "Did not see wanted errors: wanted: " + wanted + ", got: " + errorsAfter;
    }

    @Test
    public void testTextBlock () {
	testInput ("\"\"\"Some text\n\n   \"\"\"", javaTokens.STRING_LITERAL);
	testInput ("\"\"\"abc\\nabc\"\"\"", javaTokens.STRING_LITERAL);
    }

    @Test
    public void testNullLiteral () {
	testInput ("null", javaTokens.NULL);
	testInput ("NULL", javaTokens.IDENTIFIER);
	testInput ("null_a", javaTokens.IDENTIFIER);
    }

    @Test
    public void testBooleanLiterals () {
	testInput ("true", javaTokens.TRUE);
	testInput ("TRUE", javaTokens.IDENTIFIER);
	testInput ("false", javaTokens.FALSE);
	testInput ("FALSE", javaTokens.IDENTIFIER);
    }

    @Test
    public void testKeywords () {
	testInput ("abstract", javaTokens.ABSTRACT);
	testInput ("assert", javaTokens.ASSERT);
	testInput ("boolean", javaTokens.BOOLEAN);
	testInput ("break", javaTokens.BREAK);
	testInput ("byte", javaTokens.BYTE);
	testInput ("case", javaTokens.CASE);
	testInput ("catch", javaTokens.CATCH);
	testInput ("char", javaTokens.CHAR);
	testInput ("class", javaTokens.CLASS);
	testInput ("const", javaTokens.CONST);
	testInput ("continue", javaTokens.CONTINUE);
	testInput ("default", javaTokens.DEFAULT);
	testInput ("do", javaTokens.DO);
	testInput ("double", javaTokens.DOUBLE);
	testInput ("else", javaTokens.ELSE);
	testInput ("enum", javaTokens.ENUM);
	testInput ("extends", javaTokens.EXTENDS);
	testInput ("final", javaTokens.FINAL);
	testInput ("finally", javaTokens.FINALLY);
	testInput ("float", javaTokens.FLOAT);
	testInput ("for", javaTokens.FOR);
	testInput ("goto", javaTokens.GOTO);
	testInput ("if", javaTokens.IF);
	testInput ("implements", javaTokens.IMPLEMENTS);
	testInput ("import", javaTokens.IMPORT);
	testInput ("instanceof", javaTokens.INSTANCEOF);
	testInput ("int", javaTokens.INT);
	testInput ("interface", javaTokens.INTERFACE);
	testInput ("long", javaTokens.LONG);
	testInput ("native", javaTokens.NATIVE);
	testInput ("new", javaTokens.NEW);
	testInput ("package", javaTokens.PACKAGE);
	testInput ("private", javaTokens.PRIVATE);
	testInput ("protected", javaTokens.PROTECTED);
	testInput ("public", javaTokens.PUBLIC);
	testInput ("return", javaTokens.RETURN);
	testInput ("short", javaTokens.SHORT);
	testInput ("static", javaTokens.STATIC);
	testInput ("strictfp", javaTokens.STRICTFP);
	testInput ("super", javaTokens.SUPER);
	testInput ("switch", javaTokens.SWITCH);
	testInput ("synchronized", javaTokens.SYNCHRONIZED);
	testInput ("this", javaTokens.THIS);
	testInput ("throw", javaTokens.THROW);
	testInput ("throws", javaTokens.THROWS);
	testInput ("transient", javaTokens.TRANSIENT);
	testInput ("try", javaTokens.TRY);
	testInput ("void", javaTokens.VOID);
	testInput ("volatile", javaTokens.VOLATILE);
	testInput ("while", javaTokens.WHILE);
    }

    @Test
    public void testNonSealed () {
	testNonSealed (false);
	testNonSealed (true);
    }

    private void testNonSealed (boolean nonSealedAllowed) {
	BitSet wantedTokens = new BitSet ();
	if (nonSealedAllowed) {
	    wantedTokens.set (javaTokens.NON_SEALED.getId ());
	    testInput ("non-sealed", wantedTokens, javaTokens.NON_SEALED);
	    testInput ("non - sealed", wantedTokens, javaTokens.IDENTIFIER, javaTokens.WHITESPACE,
		       javaTokens.MINUS, javaTokens.WHITESPACE, javaTokens.IDENTIFIER);
	    testInput ("non- sealed", wantedTokens, javaTokens.IDENTIFIER, javaTokens.MINUS,
		       javaTokens.WHITESPACE, javaTokens.IDENTIFIER);
	} else {
	    testInput ("non-sealed", wantedTokens, javaTokens.IDENTIFIER, javaTokens.MINUS, javaTokens.IDENTIFIER);
	}
    }

    @Test
    public void testIntLiterals () {
	testInt ("0", 0);
	testInt ("2", 2);
	testInt ("1996", 1996);
	testInput ("1_", grammar.ERROR);
	testInt ("2147483648", -2147483648); // may seem a bit odd
	testInput ("2147483649", grammar.ERROR);
	testInt ("0372", 0372);
	testInt ("0xDada_Cafe", 0xDada_Cafe);
	testInt ("0x00_FF__00_FF", 0x00_FF__00_FF);
	testInt ("0b1111", 0b1111);
	testInput ("0x", grammar.ERROR);
	testInput ("0xffffff;", javaTokens.INT_LITERAL, javaTokens.SEMICOLON);

	// javac is a bit iffy with octal handling compared to binary
	// lets handle individual characters out of range the same here
	testInput ("08", javaTokens.INT_LITERAL, javaTokens.INT_LITERAL);
	testInput ("0b12", javaTokens.INT_LITERAL, javaTokens.INT_LITERAL);
	testInput ("1 2", javaTokens.INT_LITERAL, javaTokens.WHITESPACE, javaTokens.INT_LITERAL);
    }

    private void testInt (String toLex, int expected) {
	CharBufferLexer l = getLexer (toLex);
	testLexing (l, new BitSet (), javaTokens.INT_LITERAL);
	int val = l.getIntValue ();
	assert val == expected : "Wrong string value: " + val + ", expected: " + expected;
    }

    @Test
    public void testLongLiterals () {
	testLong ("0l", 0);
	testLong ("0777L", 0777);
	testLong ("0x100000000L", 0x100000000L);
	testLong ("2_147_483_648L", 2_147_483_648L);
	testLong ("2147483649L", 2147483649L);
	testLong ("0xC0B0L", 0xC0B0L);
    }

    private void testLong (String toLex, long expected) {
	CharBufferLexer l = getLexer (toLex);
	testLexing (l, new BitSet (), javaTokens.LONG_LITERAL);
	long val = l.getLongValue ();
	assert val == expected : "Wrong string value: " + val + ", expected: " + expected;
    }

    @Test public void testDoubleLiterals () {
	testDouble ("1d", 1d);
	testDouble ("2.", 2.);
	testDouble ("3.14", 3.14);
	testDouble ("0.0", 0.0);
	testDouble (".3", .3);
	testDouble ("1e1", 1e1);
	testDouble ("1e1_0", 1e1_0);
	testInput ("1ee", grammar.ERROR, javaTokens.IDENTIFIER);
	testDouble ("1e-9d", 1e-9d);
	testDouble ("1e137", 1e137);
	testDouble ("1_0.3_2e4_7",1_0.3_2e4_7);
	testDouble ("0xa.p2", 0xa.p2);
	testDouble ("0x1.8p1", 0x1.8p1);
	testDouble ("0x1.fffffffffffffp1023", 0x1.fffffffffffffp1023);
	testDouble ("0x1.0p-2", 0x1.0p-2);
	testInput ("1 2.0 3.14 4e5 6", javaTokens.INT_LITERAL, javaTokens.WHITESPACE, javaTokens.DOUBLE_LITERAL,
		   javaTokens.WHITESPACE, javaTokens.DOUBLE_LITERAL, javaTokens.WHITESPACE, javaTokens.DOUBLE_LITERAL,
		   javaTokens.WHITESPACE, javaTokens.INT_LITERAL);
	testDouble ("0d", 0d);
	testFloat ("0f", 0f);
    }

    private void testDouble (String toLex, double expected) {
	CharBufferLexer l = getLexer (toLex);
	testLexing (l, new BitSet (), javaTokens.DOUBLE_LITERAL);
	double val = l.getDoubleValue ();
	assert val == expected : "Wrong string value: " + val + ", expected: " + expected;
    }

    @Test
    public void testFloatLiterals () {
	testFloat ("1f", 1f);
	testFloat ("2.f", 2.f);
	testFloat ("3.14f", 3.14f);
	testFloat ("0xa.p2f", 0xa.p2f);
    }

    private void testFloat (String toLex, float expected) {
	CharBufferLexer l = getLexer (toLex);
	testLexing (l, new BitSet (), javaTokens.FLOAT_LITERAL);
	float val = l.getFloatValue ();
	assert val == expected : "Wrong string value: " + val + ", expected: " + expected;
    }

    @Test
    public void testComplex () {
	// nonsense, but valid for tokenisation
	testInput ("{    \n    (/*whatever*/)=[]}",
		   javaTokens.LEFT_CURLY, javaTokens.WHITESPACE,
		   javaTokens.LF, javaTokens.WHITESPACE,
		   javaTokens.LEFT_PARENTHESIS, javaTokens.TRADITIONAL_COMMENT,
		   javaTokens.RIGHT_PARENTHESIS, javaTokens.EQUAL,
		   javaTokens.LEFT_BRACKET, javaTokens.RIGHT_BRACKET,
		   javaTokens.RIGHT_CURLY);

	testInput ("import foo.bar.Baz;\npublic class Foo {\n\tBaz bar = 42;\n}",
		   javaTokens.IMPORT, javaTokens.WHITESPACE, javaTokens.IDENTIFIER/*foo*/, javaTokens.DOT,
		   javaTokens.IDENTIFIER/*bar*/, javaTokens.DOT, javaTokens.IDENTIFIER/*Baz*/, javaTokens.SEMICOLON,
		   javaTokens.LF, javaTokens.PUBLIC, javaTokens.WHITESPACE, javaTokens.CLASS, javaTokens.WHITESPACE,
		   javaTokens.IDENTIFIER, javaTokens.WHITESPACE, javaTokens.LEFT_CURLY, javaTokens.LF,
		   javaTokens.WHITESPACE, javaTokens.IDENTIFIER/*Baz*/, javaTokens.WHITESPACE,
		   javaTokens.IDENTIFIER/*bar*/, javaTokens.WHITESPACE, javaTokens.EQUAL, javaTokens.WHITESPACE,
		   javaTokens.INT_LITERAL, javaTokens.SEMICOLON, javaTokens.LF, javaTokens.RIGHT_CURLY);
    }

    @Test
    public void testBadInput () {
	testInput ("\\", grammar.ERROR);
	testInput ("§§", grammar.ERROR, grammar.ERROR);
    }

    @Test
    public void testNextNonWhitespaceToken () {
	testNextNonWhitespace ("", grammar.END_OF_INPUT);
	testNextNonWhitespace ("package   ", javaTokens.PACKAGE, grammar.END_OF_INPUT);
	testNextNonWhitespace ("package\n", javaTokens.PACKAGE, grammar.END_OF_INPUT);
	testNextNonWhitespace ("  \t  <  >", javaTokens.LT, javaTokens.GT, grammar.END_OF_INPUT);
    }

    private void testNextNonWhitespace (String text, Token... expected) {
	CharBufferLexer l = getLexer (text);
	for (int i = 0; i < expected.length; i++) {
	    BitSet tokens = l.nextToken (new BitSet ());
	    assert tokens.get (expected[i].getId ()) : i + ": Wrong Token: expected: " + expected[i] + ", got: " +
		toReadableString (tokens) +
		(tokens.get (grammar.ERROR.getId ()) ? ", error code: " + l.getError () : "");
	}
    }

    @Test
    public void testIgnorable () {
	CharBufferLexer l = getLexer ("v\u200Bar");
	BitSet tokens = l.nextToken (new BitSet ());
	assert tokens.get (javaTokens.IDENTIFIER.getId ());
	String value = l.getIdentifier ();
	assert value.equals ("var");
    }

    private void testInput (String text, Token... expected) {
	testInput (text, new BitSet (), expected);
    }

    private void testInput (String text, BitSet wantedTokens, Token... expected) {
	CharBufferLexer l = getLexer (text);
	testLexing (l, wantedTokens, expected);
    }

    private CharBufferLexer getLexer (String contents) {
	CharBuffer cb = CharBuffer.wrap (contents);
	return new CharBufferLexer (grammar, javaTokens, cb, Paths.get ("<string literal>"), diagnostics);
    }

    private void testLexing (CharBufferLexer l, BitSet wantedTokens, Token... expected) {
	for (int i = 0; i < expected.length; i++) {
	    assert l.hasMoreTokens () : "Too few tokens, expected: " + Arrays.toString (expected);
	    Token t = l.nextRealToken (wantedTokens);
	    assert t.equals (expected[i]) : "Wrong Token: expected: " + expected[i] + ", got: " + t.getName () +
		(t.equals (grammar.ERROR) ? ", error code: " + l.getError () : "");
	}
	if (l.hasMoreTokens ()) {
	    Token t = l.nextRealToken (wantedTokens);
	    assert t.equals (grammar.END_OF_INPUT) : "Lexer has more tokens: " + t.getName ();
	}
	assert !l.hasMoreTokens () : "Lexer has more available tokens than expected";
    }

    public String toReadableString (BitSet tokens) {
	return tokens.stream ().mapToObj (i -> grammar.getToken (i).getName ()).collect (Collectors.joining (", ", "{", "}"));
    }
}

