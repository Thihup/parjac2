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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Parser;
import org.khelekore.parjac2.parser.PredictCache;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.SyntaxTreeNode;

public class TestParser {
    private final boolean printTree;
    private final Grammar grammar = new Grammar ();
    private final Java11Tokens java11Tokens = new Java11Tokens (grammar);
    private final PredictCache predictCache = new PredictCache (grammar);
    private final Rule goalRule;

    public static void main (String[] args) throws IOException {
	if (args.length < 1) {
	    usage ();
	    return;
	}
	boolean printTree = false;
	int fileStart = 0;
	if (args[0].equals ("-print_parse")) {
	    printTree = true;
	    fileStart = 1;
	}
	TestParser tg = new TestParser (printTree);
	ExecutorService es = Executors.newFixedThreadPool (Runtime.getRuntime ().availableProcessors () + 1);
	for (int i = fileStart; i < args.length; i++) {
	    String filename = args[i];
	    es.submit (() -> tg.test(filename));
	}
	es.shutdown ();
    }

    private static void usage () {
	System.err.println ("usage: java " + TestParser.class.getName () + " [-print_parse] file_to_parse*");
    }

    public TestParser (boolean printTree) throws IOException {
	this.printTree = printTree;
	goalRule = JavaGrammarHelper.readAndValidateRules (grammar, false);
	System.out.println ("Testing parsing with " + -grammar.getMaxRuleGroupId () + " rule groups, " +
			    -grammar.getMaxRuleId () + " rules and " + grammar.getMaxTokenId () + " tokens");
    }

    public Void test (String file) throws IOException {
	System.out.println ("Testing parsing of: " + file);
	CharBuffer input = pathToCharBuffer (Paths.get (file), StandardCharsets.ISO_8859_1);
	CharBufferLexer lexer = new CharBufferLexer (grammar, java11Tokens, input);
	CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
	Parser p = new Parser (grammar, Paths.get (file), predictCache, lexer, diagnostics);
	SyntaxTreeNode tree = p.parse (goalRule);
	if (diagnostics.hasError ()) {
	    Locale locale = Locale.getDefault ();
	    diagnostics.getDiagnostics ().forEach (d -> System.err.println (d.getMessage (locale)));
	}
	if (printTree)
	    printTree (tree);
	return null;
    }

    private CharBuffer pathToCharBuffer (Path path, Charset encoding) throws IOException {
	ByteBuffer  buf = ByteBuffer.wrap (Files.readAllBytes (path));
	CharsetDecoder decoder = encoding.newDecoder ();
	decoder.onMalformedInput (CodingErrorAction.REPORT);
	decoder.onUnmappableCharacter (CodingErrorAction.REPORT);
	return decoder.decode (buf);
    }

    private void printTree (SyntaxTreeNode tree) {
	printTree (tree, "");
    }

    private void printTree (SyntaxTreeNode n, String indent) {
	System.out.print (indent);
	System.out.print (n.getId ());
	if (n.getValue () != null)
	    System.out.print ("(" + n.getValue () + ")");
	System.out.println ();
	n.getChildren ().forEach (c -> printTree (c, indent + " "));
    }
}