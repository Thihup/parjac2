package org.khelekore.parjac2.javacompiler.parse;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.TestParserHelper;
import org.khelekore.parjac2.parser.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestConstructorParsing {

    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("ConstructorDeclaration", false);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSimpleConstructor () {
	testSuccessfulParse ("Foo () {}");
	testSuccessfulParse ("Foo () {super();}");
	testSuccessfulParse ("Foo (int a) {super();}");
	testSuccessfulParse ("Foo (int a) {super(a);}");
	testSuccessfulParse ("Foo (int a) {this();}");
	testSuccessfulParse ("Foo (int a) {this(a);}");
    }

    @Test
    public void testAnnotatedConstructor () {
	testSuccessfulParse ("@Bar Foo () {}");
	testSuccessfulParse ("@Bar @Baz Foo () {}");
    }

    @Test
    public void testModifiersConstructor () {
	testSuccessfulParse ("public Foo () {}");
	testSuccessfulParse ("protected Foo () {}");
	testSuccessfulParse ("private Foo () {}");

	testSuccessfulParse ("@Bar public Foo () {}");
	testSuccessfulParse ("@Bar protected Foo () {}");
	testSuccessfulParse ("@Bar private Foo () {}");
    }

    @Test
    public void testConstructorThatThrows () {
	testSuccessfulParse ("Foo () throws A {}");
	testSuccessfulParse ("Foo () throws A, B {}");
    }

    @Test
    public void testGenericConstructor () {
	testSuccessfulParse ("<T> Foo () {}");
	testSuccessfulParse ("<T> Foo (T t) {}");
	testSuccessfulParse ("<K, V> Foo () {}");
    }

    @Test
    public void testGenericInvocations () {
	testSuccessfulParse ("Foo () {<T>this();}");
	testSuccessfulParse ("Foo () {<T>super();}");
    }

    private void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }
}
