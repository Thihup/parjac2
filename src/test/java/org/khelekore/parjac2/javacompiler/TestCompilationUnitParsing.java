package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestCompilationUnitParsing {

    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("CompilationUnit", false);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testHelloWorld () {
	testSuccessfulParse ("""
			     class HW {
                                 public static void main (String... args) {
				     System.out.println("Hello World!");
                                 }
                             }""");
	testSuccessfulParse ("""
			     class HW {
                                 public static void main (String[] args) {
				     System.out.println("Hello World!");
                                 }
                             }""");
    }

    @Test
    public void testInterface () {
	testSuccessfulParse ("interface I { }");
	testSuccessfulParse ("public interface I { }");
	testSuccessfulParse ("public interface I<T> { }");
	testSuccessfulParse ("public interface I extends J { }");
	testSuccessfulParse ("public interface I extends J, K, L { }");
	testSuccessfulParse ("public interface I<T> extends J, K, L { }");
	testSuccessfulParse ("interface CP { void split (Parts parts); }");
	testSuccessfulParse ("interface CP { int i = 3; }");
	testSuccessfulParse ("interface CP { int i = 3, j = 4; boolean b(Foo foo, Bar bar);}");
	testSuccessfulParse ("interface CP { default void foo () { }}");
    }

    @Test
    public void testClasses () {
	testSuccessfulParse ("class A {}");
	testSuccessfulParse ("public final class A {}");
	testSuccessfulParse ("public final class A<T> {}");
	testSuccessfulParse ("public final class A extends B {}");
	testSuccessfulParse ("public final class A<T> extends B {}");
	testSuccessfulParse ("public final class A implements B {}");
	testSuccessfulParse ("public final class A implements B, C, D {}");
	testSuccessfulParse ("public final class A<T> implements B {}");
	testSuccessfulParse ("public final class A<T> extends B implements C {}");
    }

    @Test
    public void testConstructors () {
	testSuccessfulParse ("class A { A () { i = 1; }}");
	testSuccessfulParse ("class A { A () { this (1); }}");
	testSuccessfulParse ("class A { A () { super (1); }}");
	testSuccessfulParse ("class A { public A () { i = 1; }}");
	testSuccessfulParse ("class A { A () { this (1); j = 2; }}");
	testSuccessfulParse ("class A { A () { super (1); j = 2; }}");
    }


    @Test
    public void testEmptyClassMember () {
	testSuccessfulParse ("class A { ; }");
    }

    @Test
    public void testEmptyEnumMember () {
	testSuccessfulParse ("enum A { ; ; }");
    }

    @Test
    public void testEmptyInterfaceMember () {
	testSuccessfulParse ("interface A { ; }");
    }

    @Test
    public void testEmptyAnnotationTypeMember () {
	testSuccessfulParse ("@interface A { ; }");
    }

    @Test
    public void testStaticInitializer () {
	testSuccessfulParse ("class A { static { i = 1; } }");
	testSuccessfulParse ("class A { static { i = 1; } }");
	testSuccessfulParse ("class A { static { i = 1; } static { j = 1; } }");
    }

    @Test
    public void testMultipleErrors () {
	testFailedParse ("class A { void a () { int i int j int k }}", 3);
    }

    @Test
    public void testMissingManyTokens () {
	testFailedParse ("package", 2);
    }

    private void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }

    private void testFailedParse (String s, int expectedErrors) {
	TestParserHelper.testFailedParse (g, s, diagnostics, expectedErrors);
    }
}
