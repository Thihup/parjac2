package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestTypeParsing {

    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("Type", false);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSimpleBasic () {
	testSuccessfulParse ("byte");
	testSuccessfulParse ("short");
	testSuccessfulParse ("int");
	testSuccessfulParse ("long");
	testSuccessfulParse ("float");
	testSuccessfulParse ("double");
	testSuccessfulParse ("boolean");
	testSuccessfulParse ("Foo");
    }

    @Test
    public void testLongName () {
	testSuccessfulParse ("foo.Bar");
	testSuccessfulParse ("foo.bar.Baz");
	testSuccessfulParse ("foo.bar.baz.Quod");
    }

    @Test
    public void testAnnotated () {
	testSuccessfulParse ("@foo byte");
	testSuccessfulParse ("@foo(bar) byte");
	testSuccessfulParse ("@foo(bar={a, b, c}) byte");
	testSuccessfulParse ("@foo(@bar) byte");
    }

    @Test
    public void testArrays () {
	testSuccessfulParse ("int[]");
	testSuccessfulParse ("int[][]");
	testSuccessfulParse ("int[][][]");
    }

    @Test
    public void testTyped () {
	testSuccessfulParse ("Foo<T>");
	testSuccessfulParse ("Foo<K, V>");
    }

    private void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }
}
