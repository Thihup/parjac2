package org.khelekore.parjac2.javacompiler.semantic;

import org.testng.annotations.Test;

public class TestReturnChecker extends TestCompilationErrorHandling {

    @Test
    public void testUnreachableCode () {
	testClass ("C.java", "class C { void a () { int x = 0; return; x++; }}", 1);
    }

    @Test
    public void testBlockInsideMethod () {
	testClass ("C.java", "class C { int a () { { return 3; } return 4; }}", 1);
    }

    @Test
    public void testIfWithReturnDoesNotMarkUnreachable () {
	testClass ("C.java", "class C { void a (int x) { if (x == 0) return; x++; }}", 0);
    }

    @Test
    public void testTrueIfWithReturnRequiresRerturnAfter () {
	testClass ("C.java", "class C { int a () { if (true) return 1; }}", 1);
    }

    @Test
    public void testTrueIfWithReturnAfterGivesWarning () {
	testClass ("C.java", "class C { int a () { if (true) return 1; return 2; }}", 0, 1);
    }

    @Test
    public void testIfElseBothReturnShouldMarkCodeAfterAsUnreachable () {
	testClass ("C.java", "class C { int a (int x) { if (x == 0) return 1; else return 2; x++; }}", 1);
    }

    @Test
    public void testIfElseBothThrowShouldMarkCodeAfterAsUnreachable () {
	testClass ("C.java", "class C { int a (int x) { if (x == 0) throw new RuntimeException (); else throw new RuntimeException (); x++; }}", 1);
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

    @Test
    public void testResource () {
	testClass ("C.java", "import java.io.*; class C { void foo () throws java.io.IOException {" +
		   "try (InputStream is = new FileInputStream(\"/tmp/foo\")) { int i = is.read (); }}}", 0);
	testClass ("C.java", "import java.io.*; class C { void foo () throws java.io.IOException {" +
		   "try (InputStream is = new FileInputStream(\"/tmp/foo\"); BufferedReader br = new BufferedReader (is)) {" +
		   "String s = br.readLine (); }}}", 0);
	testClass ("C.java", "class C { void foo () { try (String s = bar ()) { int i = s.length (); }} String bar () { return \"\"; }}", 1);
    }

    @Test
    public void testExceptionNeedsToBeDeclared () {
	testClass ("C.java", "class C { void foo () throws InterruptedException { throw new InterruptedException (); }}", 0);
	testClass ("C.java", "import java.io.IOException; class C { void a () throws IOException { throw new IOException (); }}", 0);
	testClass ("C.java", "import java.io.IOException; class C { void a () { throw new IOException (); }}", 1);
    }

    @Test
    public void testRuntimeExceptionsAreAllowed () {
	testClass ("C.java", "class C { void foo () { throw new RuntimeException (); }}", 0);
	testClass ("C.java", "class C { void foo () { throw new InterruptedException (); }}", 1);
    }

    @Test
    public void testThrowingNonExceptionGivesErrors () {
	testClass ("C.java", "class C { static void a () { throw Integer.valueOf (2); }}", 1);
    }

    @Test
    public void testCaughtExceptionsAreAllowed () {
	testClass ("C.java", "class C { void foo () { try { throw new InterruptedException (); } catch (InterruptedException e) {} }}", 0);
    }

    @Test
    public void testThrownExceptionIsNotCaught () {
	testClass ("C.java", "class C { void foo () { try { throw new InterruptedException (); } catch (NullPointerException e) {} }}", 1);
    }

    @Test
    public void testThrowInLambda () {
	testClass ("C.java", "class C { void foo () { Runnable r = () -> { }; }}", 0);
	testClass ("C.java",
		   "interface I { void x () throws InterruptedException;} " +
		   "class C { void foo () { I i = () -> { throw new InterruptedException (); }; }}", 0);
	testClass ("C.java", "class C { void foo () { Runnable r = () -> { throw new java.io.IOException ();}; }}", 1);
    }
}
