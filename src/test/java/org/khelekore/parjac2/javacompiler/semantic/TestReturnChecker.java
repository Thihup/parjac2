package org.khelekore.parjac2.javacompiler.semantic;

import org.testng.annotations.Test;

public class TestReturnChecker extends TestCompilationErrorHandling {

    @Test
    public void testUnreachableCode () {
	testClass ("C.java", "class C { void a () { int x = 0; return; x++; }}", 1);
    }

    @Test
    public void testIfWithReturnDoesNotMarkUnreachable () {
	testClass ("C.java", "class C { void a (int x) { if (x == 0) return; x++; }}", 0);
    }

    @Test
    public void testIfElseBothReturnShouldMarkCodeAfterAsUnreachable () {
	testClass ("C.java", "class C { int a (int x) { if (x == 0) return 1; else return 2; x++; }}", 1);
    }

    @Test
    public void testInfiniteLoopMeansMethodDoesNotHaveToReturn () {
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (;;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; true;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; !false;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; !!true;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; 1 == 1;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; 1 + 2 == 3;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; 1 * 3 == 3;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; 12 / 4 == 3;) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; \"a\" == \"a\";) System.in.read (); }}", 0);
	testClass ("C.java", "class C { int foo () throws java.io.IOException { for (; \"a\" + \"b\" == \"ab\";) System.in.read (); }}", 0);
    }

    @Test
    public void testLoopExpressionUsingVariablesIsNotConsideredInfinite () {
	testClass ("C.java", "class C { int foo () throws java.io.IOException { boolean b = true; int x = 0; for (; b;) System.in.read (); }}", 1);
    }

    @Test
    public void testCodeAfterInfiniteLoopIsUnreachable () {
	testClass ("C.java", "class C { int foo () throws java.io.IOException { int x = 0; for (;;) System.in.read (); return x; }}", 1);
    }
}
