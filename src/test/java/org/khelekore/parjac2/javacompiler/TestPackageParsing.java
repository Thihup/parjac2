package org.khelekore.parjac2.javacompiler;

import java.util.List;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.MarkerAnnotation;
import org.khelekore.parjac2.javacompiler.syntaxtree.PackageDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeName;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parsetree.TokenNode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestPackageParsing {

    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("PackageDeclaration", false);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSinglePackageMissingSemiColon () {
	TestParserHelper.testFailedParse (g, "package foo", diagnostics, 1);
    }

    @Test
    public void testSinglePackage () {
	List<String> names = List.of ("foo");
	TestParserHelper.testSuccessfulParse (g, "package foo;", diagnostics,
					      new PackageDeclaration (null, null, names));
    }

    @Test
    public void testMultiPackage () {
	List<String> names = List.of ("foo", "bar", "baz");
	TestParserHelper.testSuccessfulParse (g, "package foo.bar.baz;", diagnostics,
					      new PackageDeclaration (null, null, names));
    }

    @Test
    public void testMarkerAnnotatedPackage () {
	List<String> foo = List.of ("foo");
	List<String> bar = List.of ("Bar");
	TokenNode at = new TokenNode(TestParserHelper.getTokens ().AT, null);
	MarkerAnnotation maf = new MarkerAnnotation (null, at, new TypeName (null, foo));
	MarkerAnnotation mab = new MarkerAnnotation (null, at, new TypeName (null, bar));
	TestParserHelper.testSuccessfulParse (g, "@foo package foo;", diagnostics,
					      new PackageDeclaration (null, List.of (maf), foo));
	TestParserHelper.testSuccessfulParse (g, "@foo @Bar package foo;", diagnostics,
					      new PackageDeclaration (null, List.of (maf, mab), foo));
    }

    @Test
    public void testSingleElementAnnotatedPackage () {
	testSuccessfulParse ("@foo(bar) package foo;");
    }

    @Test
    public void testNormalAnnotatedPackage () {
	testSuccessfulParse ("@foo() package foo;");
    }

    private void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }
}
