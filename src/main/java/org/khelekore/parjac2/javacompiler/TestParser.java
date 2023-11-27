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
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.Dims;
import org.khelekore.parjac2.javacompiler.syntaxtree.DottedName;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclarator;
import org.khelekore.parjac2.javacompiler.syntaxtree.PrimitiveType;
import org.khelekore.parjac2.javacompiler.syntaxtree.SimpleRecordComponent;
import org.khelekore.parjac2.javacompiler.syntaxtree.TwoPartExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.UntypedMethodInvocation;
import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclaratorId;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Parser;
import org.khelekore.parjac2.parser.PredictCache;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class TestParser {
    private final Charset charset;
    private final boolean fillInClasses;
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
	boolean fillInClasses = false;
	int fileStart = 0;
	if (args[fileStart].equals ("-encoding") && args.length > fileStart + 1) {
	    charset = Charset.forName (args[++fileStart]);
	    fileStart++;
	}
	if (args[fileStart].equals ("-fill_in_classes")) {
	    fillInClasses = true;
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

	TestParser tg = new TestParser (charset, fillInClasses, printParseTree, printSyntaxTree);
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

    public TestParser (Charset charset, boolean fillInClasses, boolean printParseTree, boolean printSyntaxTree) throws IOException {
	this.charset = charset;
	this.fillInClasses = fillInClasses;
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

	    if (fillInClasses) {
		CompilationArguments settings = new CompilationArguments ();
		ClassInformationProvider cip = new ClassInformationProvider (diagnostics, settings);
		cip.scanClassPath ();
		cip.addTypes (syntaxTree, filePath);
		ClassSetter.fillInClasses (javaTokens, cip, List.of (new ParsedEntry (dirAndPath, syntaxTree)), diagnostics);
	    }

	    if (printSyntaxTree) {
		printTree (syntaxTree);
	    }
	} catch (Throwable t) {
	    t.printStackTrace ();
	}
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
	if (n instanceof DottedName dn)
	    n = dn.replaced ();
	System.out.print (indent);
	ParsePosition pos = n.position ();
	String shortPos = pos != null ? pos.toShortString () : "-";
	System.out.print (n.getId () + " " + shortPos + ", class: " + n.getClass ().getName ());
	printValue (n);
	System.out.println ();
	n.visitChildNodes (c -> printTree (c, indent + " "));
    }

    private void printValue (ParseTreeNode n) {
	Object res =
	    switch (n) {
	    case DottedName dn -> n.getValue ();
	    case VariableDeclaratorId dn -> n.getValue ();
	    case MethodDeclarator dn -> n.getValue ();
	    case SimpleRecordComponent dn -> n.getValue ();
	    case UntypedMethodInvocation m -> m.getMethodName ();
	    case TypeDeclaration td -> td.getName ();
	    case PrimitiveType pt -> pt.type ().getName ();
	    case Dims d -> "[]".repeat (d.rank ());
	    case TokenNode s -> s.getValue ();
	    case TwoPartExpression t -> t.token ();
	    default -> null;
	    };
	if (res != null)
	    System.out.print (" " + res.toString ());
    }
}
