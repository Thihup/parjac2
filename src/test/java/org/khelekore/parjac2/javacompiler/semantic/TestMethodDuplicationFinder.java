package org.khelekore.parjac2.javacompiler.semantic;

import org.testng.annotations.Test;

public class TestMethodDuplicationFinder extends TestCompilationErrorHandling {
    @Test
    public void testSameNameEmptyArgumentsGivesError () {
	testClass ("C.java", "class C { int x() {} int x () {} }", 1);
    }

    @Test
    public void testSameArgumentsButDifferentResultsGiveError () {
	testClass ("C.java", "class C { int x() {} double x () {} }", 1);
    }

    @Test
    public void testSimpleNameFQNAsArguments () {
	testClass ("C.java", "class C { int x(String s) {} double x (java.lang.String s) {} }", 1);
    }

    @Test
    public void testDifferentClassesButSameNameWorks () {
	testClass ("C.java", "class List {} class C { int x(List ls) {return 0;} double x (java.util.List s) {return 0;} }", 0);
    }

    @Test
    public void testDuplicateConstructorsGivesErrors () {
	testClass ("C.java", "class C { C (int x) {} C (int y) {}}", 1);
    }

    @Test
    public void testMultipleCompactConstructorsGivesError () {
	testClass ("C.java", "record C(int x, int y) { C {} C {}}", 1);
    }

    @Test
    public void testCompactConstructorClashWithNormalConstructor () {
	testClass ("C.java", "record C(int x, int y) { C {} }", 0);
	testClass ("C.java", "record C(int x, int y) { C {} C (int x, int y) {}}", 1);
    }

    @Test
    public void testMultipleInitBlocksWorks () {
	testClass ("C.java", "class C { {} {} }", 0);
    }

    @Test
    public void testMultipleInStaticBlocksWorks () {
	testClass ("C.java", "class C { static {} static {} }", 0);
    }
}
