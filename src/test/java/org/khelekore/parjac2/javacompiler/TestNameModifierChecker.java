package org.khelekore.parjac2.javacompiler;

import org.testng.annotations.Test;

public class TestNameModifierChecker extends TestCompilationErrorHandling {

    @Test
    public void testExtendingFinalClassGivesError () {
	testClass ("C.java", "class C extends String {}", 1);
    }

    @Test
    public void testExtendingFinalCompiledClassGivesError () {
	testClass ("C.java", "final class B {} class C extends B {}", 1);
    }

    @Test
    public void testNonClassesDoNotGiveErrors () {
	testClass ("E.java", "enum E { Y, N}", 0);
	testClass ("R.java", "record R (int x) {}", 0);
	testClass ("I.java", "interface I extends java.io.Serializable {}", 0);
    }

    @Test
    public void testPublicClassInWrongFileGivesError () {
	testClass ("X.java", "public class C {}", 1);
    }

    @Test
    public void testProtectedTopLevelGivesError () {
	testClass ("C.java", "protected class C {}", 1);
    }

    @Test
    public void testPrivateTopLevelGivesError () {
	testClass ("C.java", "private class C {}", 1);
    }

    @Test
    public void testTooManyClassAccessFlagsGivesError () {
	testClass ("C.java", "public protected class C {}", 1);
    }

    @Test
    public void testTooManyFieldAccessFlagsGivesError () {
	testClass ("C.java", "class C { public private int x; }", 1);
    }

    @Test
    public void testFieldFinalAndVolatileGivesError () {
	testClass ("C.java", "class C { final volatile int x; }", 1);
    }

    @Test
    public void testConstructorWithTooManyAccessFlagsGivesError () {
	testClass ("C.java", "class C { public private C () {} }", 1);
    }

    @Test
    public void testMethodNativeStrictGivesError () {
	testClass ("C.java", "class C { native strictfp int x  () {} }", 1);
    }

    @Test
    public void testMethodAbstractPrivateGivesEror () {
	testClass ("C.java", "class C { abstract private int x  (); }", 1);
    }

    @Test
    public void testMethodAbstractStaticGivesEror () {
	testClass ("C.java", "class C { abstract static int x  (); }", 1);
    }

    @Test
    public void testMethodAbstractFinalGivesEror () {
	testClass ("C.java", "class C { abstract final int x  (); }", 1);
    }

    @Test
    public void testMethodAbstractNativeGivesEror () {
	testClass ("C.java", "class C { abstract native int x  (); }", 1);
    }

    @Test
    public void testMethodAbstractStrictGivesEror () {
	testClass ("C.java", "class C { abstract strictfp int x  (); }", 1);
    }

    @Test
    public void testMethodAbstractSynchronizedGivesEror () {
	testClass ("C.java", "class C { abstract synchronized int x  (); }", 1);
    }

    @Test
    public void testMethodAbstractWithBodyGivesError () {
	testClass ("C.java", "class C { abstract int x (); }", 0);
	testClass ("C.java", "class C { abstract int x () {} }", 1);
    }

    @Test
    public void testMethodNativeWithBodyGivesError () {
	testClass ("C.java", "class C { native int x (); }", 0);
	testClass ("C.java", "class C { native int x () {} }", 1);
    }

    @Test
    public void testMethodEmptyBodyGivesError () {
	testClass ("C.java", "class C { int x (); }", 1);
	diagnostics.clear ();
	testClass ("C.java", "class C { private int x (); }", 1);
    }
}
