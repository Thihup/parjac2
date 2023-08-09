package org.khelekore.parjac2.javacompiler;

import java.util.List;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.MarkerAnnotation;
import org.khelekore.parjac2.javacompiler.syntaxtree.PackageDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.SyntaxTreeNode;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeName;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
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
	testFailedParse ("package foo");
    }

    @Test
    public void testSinglePackage () {
	List<String> names = List.of ("foo");
	testSuccessfulParse ("package foo;", new PackageDeclaration (null, null, names));
    }

    @Test
    public void testMultiPackage () {
	List<String> names = List.of ("foo", "bar", "baz");
	testSuccessfulParse ("package foo.bar.baz;", new PackageDeclaration (null, null, names));
    }

    @Test
    public void testMarkerAnnotatedPackage () {
	List<String> foo = List.of ("foo");
	List<String> bar = List.of ("Bar");
	MarkerAnnotation maf = new MarkerAnnotation (null, new TypeName (null, foo));
	MarkerAnnotation mab = new MarkerAnnotation (null, new TypeName (null, bar));
	testSuccessfulParse ("@foo package foo;",
			     new PackageDeclaration (null, List.of (maf), foo));
	testSuccessfulParse ("@foo @Bar package foo;",
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
	testSuccessfulParse (s, null);
    }

    private void testSuccessfulParse (String s, SyntaxTreeNode tn) {
	ParseTreeNode t = TestParserHelper.syntaxTree (g, s, diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParserHelper.getParseOutput (diagnostics);
	assert t != null : "Expected non null tree";
	if (tn != null) {
	    assert tn.equals (t) : "Expected: " + tn + ", but got: " + t;
	}
    }

    private void testFailedParse (String s) {
	try {
	    TestParserHelper.parse (g, s, diagnostics);
	    assert diagnostics.errorCount () > 0 : "Failed to detect errors";
	} finally {
	    diagnostics.clear ();
	}
    }
}
