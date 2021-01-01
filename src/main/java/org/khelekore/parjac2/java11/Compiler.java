package org.khelekore.parjac2.java11;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilationException;
import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Parser;
import org.khelekore.parjac2.parser.PredictCache;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

/** The actual compiler
 */
public class Compiler {
    private final CompilerDiagnosticCollector diagnostics;
    private final Grammar grammar;
    private final Java11Tokens java11Tokens;
    private final Rule goalRule;
    private final CompilationArguments settings;
    private final PredictCache predictCache;
    private final SyntaxTreeBuilder stb;
    private final ClassInformationProvider cip;

    public Compiler (CompilerDiagnosticCollector diagnostics, Grammar grammar,
		     Java11Tokens java11Tokens, Rule goalRule, CompilationArguments settings) {
	this.diagnostics = diagnostics;
	this.grammar = grammar;
	this.java11Tokens = java11Tokens;
	this.goalRule = goalRule;
	this.settings = settings;
	this.predictCache = new PredictCache (grammar);
	stb = new SyntaxTreeBuilder (diagnostics, java11Tokens, grammar);
	cip = new ClassInformationProvider (diagnostics, settings);
    }

    public void compile () {
	SourceProvider sourceProvider = settings.getSourceProvider ();
	runTimed (() -> setupSourceProvider (sourceProvider), "Setting up sources");
	if (diagnostics.hasError ())
	    return;
	if (settings.getReportTime ())
	    System.out.format ("Found %d source files\n", sourceProvider.getSourcePaths ().size ());

	List<ParseTreeNode> trees = runTimed (() -> parse (sourceProvider), "Parsing");
	if (diagnostics.hasError ())
	    return;

	runTimed (() -> cip.scanClassPath (), "Scanning classpath");
	if (diagnostics.hasError ())
	    return;
	if (settings.getReportTime ())
	    System.out.format ("Found %d classes in classpaths\n", cip.getClasspathEntrySize ());

	runTimed (() -> addTypes (trees), "Collecting compiled types");

	checkSemantics (trees);
	if (diagnostics.hasError ())
	    return;

	optimize (trees);

	runTimed (() -> createOutputDirectories (trees, settings.getClassWriter()),
		  "Creating output directories");
	if (diagnostics.hasError ())
	    return;

	runTimed (() -> writeClasses (trees), "Writing classes");
    }

    private void setupSourceProvider (SourceProvider sourceProvider) {
	try {
	    sourceProvider.setup (diagnostics);
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to setup SourceProvider: ", sourceProvider));
	}
    }

    private List<ParseTreeNode> parse (SourceProvider sourceProvider) {
	return
	    sourceProvider.getSourcePaths ().parallelStream ().
	    map (p -> parse (sourceProvider, p)).
	    filter (p -> p != null).
	    collect (Collectors.toList ());
    }

    private ParseTreeNode parse (SourceProvider sourceProvider, Path path) {
	try {
	    long start = System.nanoTime ();
	    if (settings.getDebug ())
		System.out.println ("parsing: " + path);
	    CharBuffer charBuf = sourceProvider.getInput (path);
	    CharBufferLexer lexer = new CharBufferLexer (grammar, java11Tokens, charBuf);
	    Parser parser = new Parser (grammar, path, predictCache, lexer, diagnostics);
	    ParseTreeNode tree = parser.parse (goalRule);
	    ParseTreeNode syntaxTree = stb.build (path, tree);
	    long end = System.nanoTime ();
	    if (settings.getDebug () && settings.getReportTime ())
		reportTime ("Parsing " + path, start, end);
	    return syntaxTree;
	} catch (CompilationException e) {
	    return null; // diagnostics should already have the problems
	} catch (MalformedInputException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to decode text: %s, wrong encoding?", path));
	    return null;
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to read: %s: %s", path, e));
	    return null;
	}
    }

    private void addTypes (List<ParseTreeNode> trees) {
	trees.parallelStream ().forEach (t -> cip.addTypes (t));
    }

    private void checkSemantics (List<ParseTreeNode> trees) {
	// TODO: implement
    }

    private void optimize (List<ParseTreeNode> trees) {
	// TODO: implement
    }

    private void createOutputDirectories (List<ParseTreeNode> trees, BytecodeWriter classWriter) {
	// TODO: implement
    }

    private void writeClasses (List<ParseTreeNode> trees) {
	// TODO: implement
    }

    private <T> T runTimed (CompilationStep<T> cs, String type) {
	long start = System.nanoTime ();
	T ret = cs.run ();
	long end = System.nanoTime ();
	if (settings.getReportTime ())
	    reportTime (type, start, end);
	return ret;
    }

    private void runTimed (VoidCompilationStep cs, String type) {
	long start = System.nanoTime ();
	cs.run ();
	long end = System.nanoTime ();
	if (settings.getReportTime ())
	    reportTime (type, start, end);
    }

    private void reportTime (String type, long start, long end) {
	System.out.format ("%s, time taken: %.3f millis\n", type, (end - start) / 1.0e6);
    }

    private interface CompilationStep<T> {
	T run ();
    }

    private interface VoidCompilationStep {
	void run ();
    }
}