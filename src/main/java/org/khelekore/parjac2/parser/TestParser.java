package org.khelekore.parjac2.parser;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Paths;
import java.util.Locale;
import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.JavaGrammarHelper;

public class TestParser {
    private final Grammar grammar = new Grammar ();
    private final Java11Tokens java11Tokens = new Java11Tokens (grammar);
    private final PredictCache predictCache = new PredictCache (grammar);
    private final Rule goalRule;

    public static void main (String[] args) throws IOException {
	TestParser tg = new TestParser ();
	tg.test ();
    }

    public TestParser () throws IOException {
	goalRule = JavaGrammarHelper.readAndValidateRules (grammar, false);
    }

    public void test () throws IOException {
	CharBuffer buffer = CharBuffer.wrap ("package foo;\nimport bar;\nimport baz;\nclass Foo {}");
	CharBufferLexer lexer = new CharBufferLexer (grammar, java11Tokens, buffer);
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	Parser p = new Parser (grammar, Paths.get ("Foo.java"), predictCache, lexer, diagnostics);
	p.parse (goalRule);
	if (diagnostics.hasError ()) {
	    Locale locale = Locale.getDefault ();
	    diagnostics.getDiagnostics ().forEach (d -> System.err.println (d.getMessage (locale)));
	}
    }
}