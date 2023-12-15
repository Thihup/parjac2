package org.khelekore.parjac2.javacompiler.parse;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.TestParserHelper;
import org.khelekore.parjac2.parser.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestSwitchStatement {

    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("SwitchStatement", false);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @BeforeMethod
    public void clearDiagnostics () {
	diagnostics.clear ();
    }

    @Test
    public void testOldStyleSwitch () {
	testSuccessfulParse ("""
			     switch (x) {
			     case 5: break;
			     default: break;
			     }
			     """);
    }

    @Test
    public void testOldStyleMultiLabels () {
	testSuccessfulParse ("""
			     switch (x) {
			     case 5:
			     case 6:
			     case 7:
				 break;
			     case 8:
			     case 9:
			     }
			     """);
    }

    @Test
    public void testSimpleSwitchRule () {
	testSuccessfulParse ("""
			     switch (x) {
			     case 5 -> {}
			     default -> {}
			     }
			     """);
    }

    @Test
    public void testSwitchRuleNull () {
	testSuccessfulParse ("""
			     switch (x) {
			     case 5 -> {}
			     case null -> {}
			     default -> {}
			     }
			     """);
    }

    @Test
    public void testSwitchRuleNullandDefault () {
	testSuccessfulParse ("""
			     switch (x) {
			     case 5 -> {}
			     case null, default -> {}
			     }
			     """);
    }

    @Test
    public void testMultiConstant () {
	testSuccessfulParse ("""
			     switch (x) {
			     case 5,6,7 -> {}
			     default -> {}
			     }
			     """);
    }

    @Test
    public void testSwitchPattern () {
	testSuccessfulParse ("""
			     switch (x) {
			     case Integer i when i > 5 -> {}
			     case Integer i -> {}
			     default -> {}
			     }
			     """);
    }

    @Test
    public void testIllegalDefault () {
	TestParserHelper.syntaxTree (g, """
				    switch (x) {
				    case default -> {}
				    }
				     """,
				     diagnostics, 1);
    }

    @Test
    public void testIllegalNull () {
	TestParserHelper.syntaxTree (g, """
				    switch (x) {
				    case 5, null -> {}
				    }
				     """,
				     diagnostics, 1);
    }

    @Test
    public void testIllegalDefaultAfterConstant () {
	TestParserHelper.syntaxTree (g, """
				    switch (x) {
				    case 5, default -> {}
				    }
				     """,
				     diagnostics, 1);
    }

    private void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }
}
