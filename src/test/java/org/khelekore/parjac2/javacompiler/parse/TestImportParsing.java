package org.khelekore.parjac2.javacompiler.parse;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.TestParserHelper;
import org.khelekore.parjac2.parser.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestImportParsing {

    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("ImportDeclaration", true);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSingleTypeImportDeclarationClass () {
	testSuccessfulParse ("import Random;");
    }

    @Test
    public void testSingleTypeImportDeclarationPackages () {
	testSuccessfulParse ("import java.util.Random;");
    }

    @Test
    public void testTypeImportOnDemandDeclarationOnePackage () {
	testSuccessfulParse ("import java.*;");
    }

    @Test
    public void testTypeImportOnDemandDeclarationManyPackages () {
	testSuccessfulParse ("import java.util.*;");
    }

    @Test
    public void testSingleStaticImportDeclaration () {
	testSuccessfulParse ("import static java.lang.Math.abs;");
    }

    @Test
    public void testStaticImportOnDemandDeclaration () {
	testSuccessfulParse ("import static java.lang.Math.*;");
    }

    @Test
    public void testMany () {
	testSuccessfulParse ("import Foo;\n" +
			     "import Bar;\n" +
			     "import foo.bar.Baz;" +
			     "import foo.bar.baz.*;");
    }

    @Test
    public void testMissingSemiColon () {
	TestParserHelper.testFailedParse (g, "import Foo", diagnostics, 1);
    }

    @Test
    public void testExtraStar () {
	TestParserHelper.testFailedParse (g, "import foo.*.*;", diagnostics, 3);
    }

    @Test
    public void testStarIdentifier () {
	TestParserHelper.testFailedParse (g, "import *.Foo;", diagnostics, 5);
    }

    private void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }
}
