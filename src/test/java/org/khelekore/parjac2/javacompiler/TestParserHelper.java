package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.SyntaxTreeNode;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Lexer;
import org.khelekore.parjac2.parser.Parser;
import org.khelekore.parjac2.parser.PredictCache;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TestParserHelper {

    private static final Grammar baseGrammar = new Grammar ();
    private static final JavaTokens javaTokens;
    private static Rule currentGoal;

    static {
	try {
	    JavaGrammarHelper.readRules (baseGrammar, false);
	    javaTokens = new JavaTokens (baseGrammar);
	} catch (IOException e) {
	    throw new RuntimeException ("Failed to read grammar", e);
	}
    }

    public static Grammar getJavaGrammarFromFile (String goalRule, boolean allowMany) {
	Grammar g = new Grammar (baseGrammar);
	if (allowMany) {
	    String multiGoalRule = "GOALP";
	    // use our own custom zero or more here
	    Rule r1 = g.addRule (multiGoalRule, new int[]{g.getRuleGroupId (goalRule)});
	    g.addRule (multiGoalRule, new int[]{r1.getGroupId (), g.getRuleGroupId (goalRule)});
	    goalRule = multiGoalRule;
	}
	currentGoal = g.addRule ("GOAL", new int[]{g.getRuleGroupId (goalRule), g.END_OF_INPUT.getId ()});
	try {
	    g.validateRules ();
	} catch (Throwable t) {
	    throw new RuntimeException ("Failed to validate rules", t);
	}
	return g;
    }

    public static JavaTokens getTokens () {
	return javaTokens;
    }

    public static ParseTreeNode testSuccessfulParse (Grammar g, String s,
						     CompilerDiagnosticCollector diagnostics,
						     SyntaxTreeNode tn) {
	ParseTreeNode t = syntaxTree (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParserHelper.getParseOutput (diagnostics);
	if (tn != null) {
	    assert tn.equals (t) : "Expected: " + tn + ", but got: " + t;
	}
	return t;
    }

    public static void testFailedParse (Grammar g, String s, CompilerDiagnosticCollector diagnostics, int expectedErrors) {
	try {
	    parse (g, s, diagnostics);
	    int numErrors = diagnostics.errorCount ();
	    assert numErrors == expectedErrors : "Expected " + expectedErrors + " errors, but got: " + numErrors;
	} finally {
	    diagnostics.clear ();
	}
    }

    public static ParseTreeNode syntaxTree (Grammar grammar, String s,
					    CompilerDiagnosticCollector diagnostics) {
	return syntaxTree (grammar, s, diagnostics, 0);
    }

    public static ParseTreeNode syntaxTree (Grammar grammar, String s,
					    CompilerDiagnosticCollector diagnostics,
					    int expectedErrors) {
	ParseTreeNode tree = parse (grammar, s, diagnostics);
	assert diagnostics.errorCount () == 0 : "Expected no errors: " + TestParserHelper.getParseOutput (diagnostics);
	assert tree != null : "Expected to parse to non null tree";
	SyntaxTreeBuilder stb = new SyntaxTreeBuilder (diagnostics, javaTokens, grammar);
	DirAndPath dirAndPath = new DirAndPath (Paths.get (""), Paths.get ("TestParserHelper.syntaxTree"));
	ParseTreeNode syntaxTree = stb.build (dirAndPath, tree);
	assert diagnostics.errorCount () == expectedErrors : "Expected " + expectedErrors + " errors";
	return syntaxTree;
    }

    public static ParseTreeNode parse (Grammar grammar, String s,
				       CompilerDiagnosticCollector diagnostics) {
	return parse (grammar, s, null, diagnostics);
    }

    public static ParseTreeNode parse (Grammar grammar, String s, String sourcePath,
				       CompilerDiagnosticCollector diagnostics) {
	CharBuffer charBuf = CharBuffer.wrap (s);
	Path path = Paths.get (sourcePath == null ? "TestParseHelper.getParser" : sourcePath);
	Lexer lexer = new CharBufferLexer (grammar, javaTokens, charBuf, path, diagnostics);
	PredictCache predictCache = new PredictCache (grammar);
	Parser parser = new Parser (grammar, path, predictCache, lexer, diagnostics);
	ParseTreeNode tree = parser.parse (currentGoal);
	return tree;
    }

    public static String getParseOutput (CompilerDiagnosticCollector diagnostics) {
	Locale l = Locale.getDefault ();
	return diagnostics.getDiagnostics ().
	    map (d -> d.getMessage (l)).
	    collect (Collectors.joining ("\n"));
    }
}
