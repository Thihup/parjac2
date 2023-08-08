package org.khelekore.parjac2.javacompiler;

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
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TestParser {
    private final Charset charset;
    private final boolean printParseTree;
    private final boolean printSyntaxTree;
    private final Grammar grammar = new Grammar ();
    private final JavaTokens javaTokens = new JavaTokens (grammar);
    private final PredictCache predictCache = new PredictCache (grammar);
    private final CompilerDiagnosticCollector diagnostics = new CompilerDiagnosticCollector ();
    private final SyntaxTreeBuilder stb = new SyntaxTreeBuilder (diagnostics, javaTokens, grammar);
    private final Rule goalRule;

    public static void main (String[] args) throws IOException {
	if (args.length < 1) {
	    usage ();
	    return;
	}
	Charset charset = StandardCharsets.UTF_8;
	boolean printParseTree = false;
	boolean printSyntaxTree = false;
	int fileStart = 0;
	if (args[fileStart].equals ("-encoding") && args.length > fileStart + 1) {
	    charset = Charset.forName (args[++fileStart]);
	    fileStart++;
	}
	if (args[fileStart].equals ("-print_parse")) {
	    printParseTree = true;
	    fileStart++;
	}
	if (args[fileStart].equals ("-print_syntax")) {
	    printSyntaxTree = true;
	    fileStart++;
	}
	int numThreads = Runtime.getRuntime ().availableProcessors ();
	if (args[fileStart].equals ("-single_threaded")) {
	    numThreads = 1;
	    fileStart++;
	}

	TestParser tg = new TestParser (charset, printParseTree, printSyntaxTree);
	ExecutorService es = Executors.newFixedThreadPool (numThreads);
	for (int i = fileStart; i < args.length; i++) {
	    String filename = args[i];
	    es.submit (() -> tg.test(filename));
	}
	es.shutdown ();
    }

    private static void usage () {
	System.err.println ("usage: java " + TestParser.class.getName () +
			    " [-encoding <charset>] [-print_parse] [-print_syntax] file_to_parse*");
    }

    public TestParser (Charset charset, boolean printParseTree, boolean printSyntaxTree) throws IOException {
	this.charset = charset;
	this.printParseTree = printParseTree;
	this.printSyntaxTree = printSyntaxTree;
	goalRule = JavaGrammarHelper.readAndValidateRules (grammar, false);
	System.out.println ("Testing parsing with " + -grammar.getMaxRuleGroupId () + " rule groups, " +
			    -grammar.getMaxRuleId () + " rules and " + grammar.getMaxTokenId () + " tokens");
    }

    public Void test (String file) throws IOException {
	Path filePath = Paths.get (file);
	System.out.println ("Testing parsing of: " + filePath);
	long start = System.nanoTime ();
	parse (filePath, diagnostics);
	long end = System.nanoTime ();
	if (diagnostics.hasError ()) {
	    Locale locale = Locale.getDefault ();
	    diagnostics.getDiagnostics ().forEach (d -> System.err.println (d.getMessage (locale)));
	}
	long millis = (end - start) / 1_000_000;
	System.out.println ("Done with parsing of: " + filePath + ", " + millis + " millis");
	return null;
    }

    private void parse (Path filePath, CompilerDiagnosticCollector diagnostics) throws IOException {
	try {
	CharBuffer input = pathToCharBuffer (filePath, charset);
	CharBufferLexer lexer = new CharBufferLexer (grammar, javaTokens, input, filePath, diagnostics);
	Parser p = new Parser (grammar, filePath, predictCache, lexer, diagnostics);
	ParseTreeNode parseTree = p.parse (goalRule);
	if (printParseTree && parseTree != null)
	    printTree (parseTree);
	if (diagnostics.hasError ())
	    return;

	DirAndPath dirAndPath = new DirAndPath (filePath.getParent (), filePath);
	ParseTreeNode syntaxTree = stb.build (dirAndPath, parseTree);
	if (printSyntaxTree)
	    printTree (syntaxTree);
	} catch (Throwable t) { t.printStackTrace (); }
	return;
    }

    private CharBuffer pathToCharBuffer (Path path, Charset encoding) throws IOException {
	ByteBuffer  buf = ByteBuffer.wrap (Files.readAllBytes (path));
	CharsetDecoder decoder = encoding.newDecoder ();
	decoder.onMalformedInput (CodingErrorAction.REPORT);
	decoder.onUnmappableCharacter (CodingErrorAction.REPORT);
	return decoder.decode (buf);
    }

    private void printTree (ParseTreeNode tree) {
	printTree (tree, "");
    }

    private void printTree (ParseTreeNode n, String indent) {
	System.out.print (indent);
	System.out.print (n.getId () + " " + n.getPosition ().toShortString ());
	Object v;
	if (n.isToken () && (v = n.getValue ()) != null) {
	    System.out.print (" " + v);
	}
	System.out.println ();
	n.visitChildNodes (c -> printTree (c, indent + " "));
    }
}
