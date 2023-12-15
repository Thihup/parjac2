package org.khelekore.parjac2.javacompiler.semantic;

import java.io.IOException;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.ClassInformationProvider;
import org.khelekore.parjac2.javacompiler.CompilationArguments;
import org.khelekore.parjac2.javacompiler.Compiler;
import org.khelekore.parjac2.javacompiler.InMemorySourceProvider;
import org.khelekore.parjac2.javacompiler.JavaGrammarHelper;
import org.khelekore.parjac2.javacompiler.JavaTokens;
import org.khelekore.parjac2.javacompiler.TestParserHelper;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class TestCompilationErrorHandling {

    protected Grammar grammar;
    protected Rule goalRule;
    protected JavaTokens javaTokens;
    protected InMemorySourceProvider sourceProvider;
    protected CompilationArguments settings;
    protected CompilerDiagnosticCollector diagnostics;
    protected ClassInformationProvider cip;

    @BeforeClass
    public void createTools () throws IOException {
	grammar = new Grammar ();
	goalRule = JavaGrammarHelper.readAndValidateRules (grammar, false);
	javaTokens = new JavaTokens (grammar);
	sourceProvider = new InMemorySourceProvider ();
	settings = new CompilationArguments (sourceProvider, null, null, false, false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	// start with a clean slate every time
	diagnostics = new CompilerDiagnosticCollector ();
	sourceProvider.clean ();

	cip = new ClassInformationProvider (diagnostics, settings);
	cip.scanClassPath ();
    }

    public void testClass (String filename, String text, int expectedErrors) {
	sourceProvider.input (filename, text);
	Compiler c = new Compiler (diagnostics, grammar, javaTokens, goalRule, settings);
	c.compile ();
	assert diagnostics.errorCount () == expectedErrors :
	String.format ("Wrong number of errors generated, expected: %d, got: %d: \n%s",
		       expectedErrors, diagnostics.errorCount (), TestParserHelper.getParseOutput (diagnostics));
    }
}
