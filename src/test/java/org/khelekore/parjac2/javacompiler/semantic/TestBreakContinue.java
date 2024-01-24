package org.khelekore.parjac2.javacompiler.semantic;

import org.testng.annotations.Test;

public class TestBreakContinue extends TestCompilationErrorHandling {
    @Test
    public void testBreakInFor () {
	testClass ("C.java", "class C { void foo () { for (int i = 0; i < 10; i++) { if (i == 5) break; }}}", 0);
    }

    @Test
    public void testLabeledBreakInFor () {
	testClass ("C.java", "class C { void foo () { loop: for (int i = 0; i < 10; i++) { if (i == 5) break loop; }}}", 0);
    }

    @Test
    public void testBreakOutsideLoop () {
	testClass ("C.java", "class C { void foo (int i) { if (i == 5) break; }}", 1);
    }

    @Test
    public void testBreakFromNestedClass () {
	testClass ("C.java", "class C { void foo (int i) { for (int i = 0; i < 10; i++) { " +
		   "Runnable r = new Runnable () { public void run () { break; }}; }}}", 1);
    }

    @Test
    public void testBreakFromLambda () {
	testClass ("C.java", "class C { void foo (int i) { for (int i = 0; i < 10; i++) { Runnable r = () -> { break; }; }}}", 1);
    }

}

