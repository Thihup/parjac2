package org.khelekore.parjac2.javacompiler;

import java.io.File;
import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.charset.MalformedInputException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilationException;
import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.ModuleDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Parser;
import org.khelekore.parjac2.parser.PredictCache;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.WildcardNode;

/** The actual compiler
 */
public class Compiler {
    private final CompilerDiagnosticCollector diagnostics;
    private final Grammar grammar;
    private final JavaTokens java11Tokens;
    private final Rule goalRule;
    private final CompilationArguments settings;
    private final PredictCache predictCache;
    private final SyntaxTreeBuilder stb;
    private final ClassInformationProvider cip;

    public Compiler (CompilerDiagnosticCollector diagnostics, Grammar grammar,
		     JavaTokens java11Tokens, Rule goalRule, CompilationArguments settings) {
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

	List<ParsedEntry> trees = runTimed (() -> parse (sourceProvider), "Parsing");
	if (diagnostics.hasError ())
	    return;

	runTimed (() -> cip.scanClassPath (), "Scanning classpath");
	if (diagnostics.hasError ())
	    return;
	if (settings.getReportTime ())
	    System.out.format ("Found %d classes in classpaths\n", cip.getClasspathEntrySize ());

	runTimed (() -> addTypes (trees), "Collecting compiled types");
	if (diagnostics.hasError ())
	    return;
	if (settings.getReportTime ())
	    System.out.format ("Found %d classes and %d modules to compile\n",
			       cip.getCompiledClassCount (), cip.getCompiledModuleCount ());

	checkSemantics (trees);
	if (diagnostics.hasError ())
	    return;

	optimize (trees);

	runTimed (() -> createOutputDirectories (settings.getClassWriter()),
		  "Creating output directories");
	if (diagnostics.hasError ())
	    return;

	runTimed (() -> writeClasses (settings.getClassWriter()), "Writing classes");
    }

    private void setupSourceProvider (SourceProvider sourceProvider) {
	try {
	    sourceProvider.setup (diagnostics);
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to setup SourceProvider: %s", sourceProvider));
	}
    }

    private List<ParsedEntry> parse (SourceProvider sourceProvider) {
	return
	    sourceProvider.getSourcePaths ().parallelStream ().
	    map (p -> parse (sourceProvider, p)).
	    filter (p -> p != null).
	    collect (Collectors.toList ());
    }

    private ParsedEntry parse (SourceProvider sourceProvider, DirAndPath dirAndPath) {
	Path file = dirAndPath.getFile ();
	try {
	    long start = System.nanoTime ();
	    if (settings.getDebug ())
		System.out.println ("parsing: " + file);
	    CharBuffer charBuf = sourceProvider.getInput (file);
	    CharBufferLexer lexer = new CharBufferLexer (grammar, java11Tokens, charBuf);

	    // Use our own here, we do not want to stop other classes from being parsed.
	    CompilerDiagnosticCollector collector = new CompilerDiagnosticCollector ();
	    Parser parser = new Parser (grammar, file, predictCache, lexer, collector);
	    ParseTreeNode tree = parser.parse (goalRule);
	    if (collector.hasError ()) {
		if (tree != null) {
		    // Translate to more readable errors
		    addWildcardErrors (file, tree);
		    return null;
		}
		// we could not build a tree, so return the raw parse problems as is
		diagnostics.addAll (collector);
		return null;
	    }
	    ParseTreeNode syntaxTree = stb.build (dirAndPath, tree);
	    long end = System.nanoTime ();
	    if (settings.getDebug () && settings.getReportTime ())
		reportTime ("Parsing " + file, start, end);
	    return new ParsedEntry (dirAndPath, syntaxTree);
	} catch (CompilationException e) {
	    return null; // diagnostics should already have the problems
	} catch (MalformedInputException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to decode text: %s, wrong encoding?", file));
	    return null;
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to read: %s: %s", file, e));
	    return null;
	}
    }

    private void addWildcardErrors (Path path, ParseTreeNode node) {
	if (node instanceof WildcardNode) {
	    WildcardNode w = (WildcardNode)node;
	    Token t = w.getToken ();
	    diagnostics.report (SourceDiagnostics.error (path, node.getPosition (),
							 "Expected %s", t.getName ()));
	}
	node.visitChildNodes (n -> addWildcardErrors (path, n));
    }

    private void addTypes (List<ParsedEntry> trees) {
	trees.parallelStream ().forEach (t -> cip.addTypes (t.getRoot (), t.getOrigin ()));
    }

    private void checkSemantics (List<ParsedEntry> trees) {
	// TODO: implement
    }

    private void optimize (List<ParsedEntry> trees) {
	// TODO: implement
    }

    private void createOutputDirectories (BytecodeWriter classWriter) {
	Set<Path> dirs = cip.getCompiledClasses ().stream ().map (t -> getPath (t)).collect (Collectors.toSet ());
	dirs.addAll (cip.getCompiledModules ().stream ().map (m -> getPath (m)).collect (Collectors.toSet ()));
	for (Path dir : dirs) {
	    try {
		classWriter.createDirectory (dir);
	    } catch (IOException e) {
		diagnostics.report (new NoSourceDiagnostics ("Failed to create output directory: %s", dir));
		return;
	    }
	}
    }

    private void writeClasses (BytecodeWriter classWriter) {
	cip.getCompiledClasses ().parallelStream ().forEach (td -> writeClass (classWriter, td));
	cip.getCompiledModules ().parallelStream ().forEach (td -> writeModule (classWriter, td));
    }

    private void writeClass (BytecodeWriter classWriter, TypeDeclaration td) {
	Path p = getPath (td);
	String filename = cip.getFileName (td) + ".class";
	Path result = p.resolve (filename);

	byte[] data = generateClass (cip.getOriginFile (td), td);
	// TODO: this is not full class data :-)
	write (classWriter, result, data);
    }

    private byte[] generateClass (Path origin, TypeDeclaration td) {
	BytecodeGenerator g = new BytecodeGenerator (origin, td, cip);
	return g.generate ();
    }

    private void writeModule (BytecodeWriter classWriter, ModuleDeclaration m) {
	Path p = getPath (m);
	String filename = "module-info.class";
	Path result = p.resolve (filename);
	// TODO: this is not correct module data :-)
	byte[] data = {(byte)0xca, (byte)0xfe, (byte)0xba, (byte)0xbe};
	write (classWriter, result, data);
    }

    private Path getPath (TypeDeclaration t) {
	String packageName = cip.getPackageName (t);
	String path = packageName.replace ('.', File.separatorChar);
	return Paths.get (path);
    }

    private Path getPath (ModuleDeclaration m) {
	Path p = m.getRelativePath ().getParent ();
	return p == null ? p = Paths.get (".") : p;
    }

    private void write (BytecodeWriter classWriter, Path path, byte[] data) {
	try {
	    classWriter.write (path, data);
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to create output file: %s: %s", path, e));
	    return;
	}
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