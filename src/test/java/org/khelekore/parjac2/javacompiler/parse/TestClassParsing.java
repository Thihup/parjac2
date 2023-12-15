package org.khelekore.parjac2.javacompiler.parse;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.TestParserHelper;
import org.khelekore.parjac2.parser.Grammar;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestClassParsing {

    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("TopLevelClassOrInterfaceDeclaration", false);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSimpleClasses () {
	testSuccessfulParse ("class Foo { }");
	testSuccessfulParse ("@Foo class Foo { }");
	testSuccessfulParse ("public class Foo { }");
	testSuccessfulParse ("protected class Foo { }");
	testSuccessfulParse ("private class Foo { }");
	testSuccessfulParse ("abstract class Foo { }");
	testSuccessfulParse ("final class Foo { }");
	testSuccessfulParse ("strictfp class Foo { }");
	testSuccessfulParse ("public final strictfp class Foo { }");
	testSuccessfulParse ("class Foo { private int foo; }");
	testSuccessfulParse ("class Foo { private int foo; \n" +
			     "public Foo () {}\n" +
			     "public int getFoo () {}\n" +
			     "private class Bar {}\n" +
			     "private enum Baz { ONE, TWO }\n" +
			     "}");
    }

    @Test
    public void testSimpleFailures () {
	testFailedParse ("classs Foo { }", 10); // only 2 if we allowed modules as well
	testFailedParse ("class Foo ()", 5);
	testFailedParse ("class Foo {", 1);
	testFailedParse ("class Foo {]", 5);
    }

    @Test
    public void testSimpleEnums () {
	testSuccessfulParse ("enum Foo { ONE, TWO, THREE }");
	testSuccessfulParse ("@Foo enum Foo { @Bar ONE, @Baz TWO, @Qux THREE }");
	testSuccessfulParse ("enum Foo { ONE, TWO, THREE; }");
	testSuccessfulParse ("public enum Foo { ONE, TWO, THREE; }");
	testSuccessfulParse ("private enum Foo { ONE, TWO, THREE; }");
	testSuccessfulParse ("enum Foo { ONE, TWO, THREE; \n" +
			     "private int foo; \n"+
			     "private Foo (int f) {}\n" +
			     "public void getFoo () { }\n" +
			     "}");
    }

    @Test
    public void testRecords () {
	testSuccessfulParse ("record R (int x, int y) {}");
	testSuccessfulParse ("record Point(int i, int j) implements Logging {}");
	testSuccessfulParse ("""
			     record Person(@Foo String name) {
				 Person(@Bar String name) {
				     this.name = name;
				 }
			     }
			     """);
	testSuccessfulParse ("""
                             record Rational(int num, int denom) {
                                 private static int gcd(int a, int b) {
                                     if (b == 0) return Math.abs(a);
				     else return gcd(b, a % b);
				 }

				 Rational {
				     int gcd = gcd(num, denom);
				     num    /= gcd;
				     denom  /= gcd;
				 }
			     }
			     """);
    }

    @Test
    public void testInterfaces () {
	testSuccessfulParse ("interface Foo {}");
	testSuccessfulParse ("public interface Foo {}");
	testSuccessfulParse ("protected interface Foo {}");
	testSuccessfulParse ("private interface Foo {}");
	testSuccessfulParse ("@Foo interface Foo {}");
	testSuccessfulParse ("interface Foo { public static final int FOO; }");
	testSuccessfulParse ("interface Foo { void foo (); }");
	testSuccessfulParse ("interface Foo { default void foo () { } }");
	testSuccessfulParse ("interface Foo { public default void foo () { } }");
	testSuccessfulParse ("interface Foo { public default strictfp void foo () { } }");
	testSuccessfulParse ("interface Foo { public void foo (); }");
	testSuccessfulParse ("interface Foo { public static final int FOO; public void foo (); }");
	testSuccessfulParse ("interface Foo { public strictfp default void foo () { } }");
    }

    @Test
    public void testAnnotation () {
	testSuccessfulParse ("@interface Foo {}");
	testSuccessfulParse ("public @interface Foo {}");
	testSuccessfulParse ("@Foo @interface Foo {}");

	testSuccessfulParse ("@interface ClassPreamble {\n" +
			     "String author();\n" +
			     "String date();\n" +
			     "int currentRevision() default 1;\n" +
			     "String lastModified() default \"N/A\";\n" +
			     "String lastModifiedBy() default \"N/A\";\n" +
			     "// Note use of array\n" +
			     "String[] reviewers();\n" +
			     "}");
    }

    private void testSuccessfulParse (String s) {
	TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
    }

    private void testFailedParse (String s, int expectedErrors) {
	TestParserHelper.testFailedParse (g, s, diagnostics, expectedErrors);
    }
}
