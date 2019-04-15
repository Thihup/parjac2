package org.khelekore.parjac2.java11;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Parser;
import org.khelekore.parjac2.parser.PredictCache;
import org.khelekore.parjac2.parser.Rule;

public class TestParser {
    private final Grammar grammar = new Grammar ();
    private final Java11Tokens java11Tokens = new Java11Tokens (grammar);
    private final PredictCache predictCache = new PredictCache (grammar);
    private final Rule goalRule;

    public static void main (String[] args) throws IOException {
	TestParser tg = new TestParser ();
	for (String file : args)
	    tg.test (file);
    }

    public TestParser () throws IOException {
	goalRule = JavaGrammarHelper.readAndValidateRules (grammar, false);
    }

    public void test (String file) throws IOException {
	CharBuffer input = pathToCharBuffer (Paths.get (file), StandardCharsets.UTF_8);
	CharBufferLexer lexer = new CharBufferLexer (grammar, java11Tokens, input);
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	Parser p = new Parser (grammar, Paths.get (file), predictCache, lexer, diagnostics);
	p.parse (goalRule);
	if (diagnostics.hasError ()) {
	    Locale locale = Locale.getDefault ();
	    diagnostics.getDiagnostics ().forEach (d -> System.err.println (d.getMessage (locale)));
	}
    }

    private CharBuffer pathToCharBuffer (Path path, Charset encoding) throws IOException {
	ByteBuffer  buf = ByteBuffer.wrap (Files.readAllBytes (path));
	CharsetDecoder decoder = encoding.newDecoder ();
	decoder.onMalformedInput (CodingErrorAction.REPORT);
	decoder.onUnmappableCharacter (CodingErrorAction.REPORT);
	return decoder.decode (buf);
    }
}