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
}
