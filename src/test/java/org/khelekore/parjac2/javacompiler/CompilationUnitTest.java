package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.testng.annotations.BeforeClass;

public abstract class CompilationUnitTest {
    protected Grammar g;
    protected CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("CompilationUnit", false);
    }

    public void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }

    public void testFailedParse (String s, int expectedErrors) {
	TestParserHelper.testFailedParse (g, s, diagnostics, expectedErrors);
    }
}
