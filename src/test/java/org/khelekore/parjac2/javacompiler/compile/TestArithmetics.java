package org.khelekore.parjac2.javacompiler.compile;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestArithmetics extends CompileAndRun {

    @Test
    public void testPlus () throws ReflectiveOperationException {
	testTwoOp ("+", 3, 4, 3 + 4);
	testTwoOp ("+", 3, -4, 3 + -4);
    }

    @Test
    public void testMinus () throws ReflectiveOperationException {
	testTwoOp ("-", 3, 4, 3 - 4);
    }

    @Test
    public void testMul () throws ReflectiveOperationException {
	testTwoOp ("*", 3, 4, 3 * 4);
    }

    @Test
    public void testDiv () throws ReflectiveOperationException {
	testTwoOp ("/", 3, 4, 3 / 4);
	testTwoOp ("/", 327, 4, 327 / 4);
    }

    @Test
    public void testRem () throws ReflectiveOperationException {
	testTwoOp ("%", 3, 4, 3 % 4);
	testTwoOp ("%", 327, 4, 327 % 4);
    }

    @Test
    public void testLShift () throws ReflectiveOperationException {
	testTwoOp ("<<", 3, 4, 3 << 4);
	testTwoOp ("<<", 327, 4, 327 << 4);
    }

    @Test
    public void testRShift () throws ReflectiveOperationException {
	testTwoOp (">>", 327, 4, 327 >> 4);
	testTwoOp (">>", 0xff000000, 4, 0xff000000 >> 4);
    }

    @Test
    public void testRShiftU () throws ReflectiveOperationException {
	testTwoOp (">>>", 327, 4, 327 >>> 4);
	testTwoOp (">>>", 0xff000000, 4, 0xff000000 >>> 4);
    }

    @Test
    public void testXOR () throws ReflectiveOperationException {
	testTwoOp ("^", 327, 4, 327 ^ 4);
    }

    @Test
    public void testBitAnd () throws ReflectiveOperationException {
	testTwoOp ("&", 0xff05, 0x0f04, 0xff05 & 0x0f04);
    }

    @Test
    public void testBitOr () throws ReflectiveOperationException {
	testTwoOp ("|", 0x05, 0xff00, 0x05 | 0xff00);
    }

    private void testTwoOp (String op, int a, int b, int expected) throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static public int r (int a, int b) { return a " + op + " b; }}", "r", Integer.TYPE, Integer.TYPE);
	Integer r = (Integer)m.invoke (null, a, b);
	assert r.intValue () == expected;
    }
}
