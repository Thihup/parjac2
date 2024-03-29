package org.khelekore.parjac2.javacompiler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.DiagnosticsSorter;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;

public class BatchCompiler {
    private final CompilerDiagnosticCollector diagnostics;
    private final Grammar grammar = new Grammar ();
    private final JavaTokens javaTokens = new JavaTokens (grammar);

    public static void main (String[] args) throws IOException {
	if (args.length == 0) {
	    usage ();
	    return;
	}
	CompilerDiagnosticCollector collector = new CompilerDiagnosticCollector ();
	BatchCompiler main = new BatchCompiler (collector);
	main.compile (args);
	Locale locale = Locale.getDefault ();
	collector.getDiagnostics ()
	    .sorted (new DiagnosticsSorter ())
	    .forEach (d -> System.err.println (d.getMessage (locale)));
	System.exit (collector.hasError () ? -1 : 0);
    }

    public BatchCompiler (CompilerDiagnosticCollector diagnostics) {
	this.diagnostics = diagnostics;
    }

    public void compile (String[] args) throws IOException {
	long startTime = System.nanoTime ();
	CompilationArguments settings = parseArgs (args);
	if (settings == null || diagnostics.hasError ())
	    return;

	if (diagnostics.hasError ())
	    return;

	Rule goalRule = JavaGrammarHelper.readAndValidateRules (grammar, settings.getDebug ());
	Compiler c = new Compiler (diagnostics, grammar, javaTokens, goalRule, settings);
	c.compile ();
	long endTime = System.nanoTime ();
	System.out.printf ("Time taken: %.3f seconds\n", ((endTime - startTime) / 1e9));
    }

    private CompilationArguments parseArgs (String[] args) {
	List<Path> srcDirs = new ArrayList<> ();
	BytecodeWriter output = null;
	Charset encoding = Charset.forName ("UTF-8");
	List<Path> classPathEntries = new ArrayList<> ();
	boolean reportTime = true;
	boolean debug = false;
	for (int i = 0; i < args.length; i++) {
	    switch (args[i]) {
	    case "-i":
	    case "--input":
		if (hasFollowingArgExists (args, i))
		    srcDirs.add (Paths.get (args[++i]));
	        break;
	    case "-d":
	    case "--destination":
		if (hasFollowingArgExists (args, i))
		    output = new FileBytecodeWriter (Paths.get (args[++i]));
	        break;
	    case "--encoding":
		if (hasFollowingArgExists (args, i)) {
		    String e = args[++i];
		    if (!Charset.isSupported (e)) {
			diagnostics.report (new NoSourceDiagnostics ("Unknown encoding: %s", e));
			return null;
		    }
		    encoding = Charset.forName (e);
		}
		break;
	    case "--no-timing":
		reportTime = false;
		break;
	    case "--debug":
		debug = true;
		break;
	    case "-h":
	    case "--help":
		usage ();
		return null;
	    case "-cp":
	    case "-classpath":
		if (hasFollowingArgExists (args, i)) {
		    String p = args[++i];
		    splitToClasspaths (classPathEntries, p);
		}
	        break;
	    default:
		diagnostics.report (new NoSourceDiagnostics ("Unknown argument: \"%s\" " +
							     "add \"--help\" for usage", args[i]));
		return null;
	    }
	}
	SourceProvider sp = new FileSourceProvider (srcDirs, encoding);
	CompilationArguments ca =
	    new CompilationArguments (sp, output, classPathEntries, reportTime, debug);
	ca.validate (diagnostics);
	if (diagnostics.hasError ()) {
	    System.err.println ("Invalid arguments, use \"--help\" for usage.\nProblems found:");
	    return null;
	}
	return ca;
    }

    private boolean hasFollowingArgExists (String[] args, int pos) {
	if (args.length <= (pos + 1)) {
	    diagnostics.report (new NoSourceDiagnostics ("Missing argument following: %s, pos: %d",
							 args[pos], pos));
	    return false;
	}
	return true;
    }

    private void splitToClasspaths (List<Path> classPathEntries, String classpath) {
	String[] pathParts = classpath.split (File.pathSeparator);
	for (String s : pathParts) {
	    if (s.endsWith ("/*") || s.endsWith ("\\*")) {
		try {
		    findAllJars (classPathEntries, s);
		} catch (IOException e) {
		    diagnostics.report (new NoSourceDiagnostics ("Failed to find jars for: %s (%s)",
								 s, classpath));
		}
	    } else {
		Path p = Paths.get (s);
		if (!Files.exists (p)) {
		    diagnostics.report (new NoSourceDiagnostics ("Non existing classpath: %s (%s)",
								 s, classpath));
		} else {
		    classPathEntries.add (p);
		}
	    }
	}
    }

    private void findAllJars (List<Path> classPathEntries, String dir) throws IOException {
	Path parent = Paths.get (dir).getParent (); // remove the * part
	Files.list (parent).filter (p -> isJar (p)).forEach (p -> classPathEntries.add (p));
    }

    private static boolean isJar (Path p) {
	return Files.isRegularFile (p) &&
	    p.getFileName ().toString ().toLowerCase ().endsWith (".jar");
    }

    private static void usage () {
	System.err.println ("usage: java " + BatchCompiler.class.getName () +
			    " [-cp <path>] [-classpath <path>]" + // same thing
			    " [--encoding encoding]" +
			    " [-i|--input srcdir]+ [-d|--destination dir]" +
			    " [--no-timing] [--debug] [-h|--help]");
    }
}
