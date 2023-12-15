package org.khelekore.parjac2.javacompiler.compile;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestIfs extends CompileAndRun {
    @Test
    public void testIfWithReturn () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (boolean b, int x, int y) { if (b) return x; else return y; }}",
			      "r", Boolean.TYPE, Integer.TYPE, Integer.TYPE);
	testSimpleTrueFalse (m, 72, 98);
    }

    @Test
    public void testIfWithMultiPartExpression () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (boolean b1, boolean b2) { if (b1 && b2) return 3; return 4; }}",
			      "r", Boolean.TYPE, Boolean.TYPE);
	int r = (Integer)m.invoke (null, true, false);
	assert r == 4 : "Wrong value, got: " + r + ", expected: " + 4;
	r = (Integer)m.invoke (null, true, true);
	assert r == 3 : "Wrong value, got: " + r + ", expected: " + 3;
    }

    @Test
    public void testIfWithTrippleExpression () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (boolean b1, boolean b2, boolean b3) { if (b1 && b2 && b3) return 3; return 4; }}",
			      "r", Boolean.TYPE, Boolean.TYPE, Boolean.TYPE);
	int r = (Integer)m.invoke (null, true, false, true);
	assert r == 4 : "Wrong value, got: " + r + ", expected: " + 4;
	r = (Integer)m.invoke (null, true, true, true);
	assert r == 3 : "Wrong value, got: " + r + ", expected: " + 3;
    }

    @Test
    public void testIfElseWithMultiPartExpression () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (boolean b1, boolean b2) { if (b1 || b2) return 3; else return 4; }}",
			      "r", Boolean.TYPE, Boolean.TYPE);
	int r = (Integer)m.invoke (null, false, false);
	assert r == 4;
	r = (Integer)m.invoke (null, true, false);
	assert r == 3;
    }

    @Test
    public void testReturnTernary () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (boolean b, int x, int y) { return b ? x : y; }}",
			      "r", Boolean.TYPE, Integer.TYPE, Integer.TYPE);
	testSimpleTrueFalse (m, 3, 7);
    }

    @Test
    public void testReturnComplexTernary () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (boolean b1, boolean b2) { return b1 && b2 ? 3 : 4; }}",
			      "r", Boolean.TYPE, Boolean.TYPE);
	int r = (Integer)m.invoke (null, true, false);
	assert r == 4;
	r = (Integer)m.invoke (null, true, true);
	assert r == 3;
    }

    private void testSimpleTrueFalse (Method m, int trueVal, int falseVal) throws ReflectiveOperationException {
	int r = (Integer)m.invoke (null, true, trueVal, falseVal);
	assert r == trueVal : "Got wrong number back";
	r = (Integer)m.invoke (null, false, trueVal, falseVal);
	assert r == falseVal : "Got wrong number back";
    }
}
