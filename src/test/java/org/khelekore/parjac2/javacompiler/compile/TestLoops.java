package org.khelekore.parjac2.javacompiler.compile;

import java.lang.reflect.Method;
import java.util.List;

import org.testng.annotations.Test;

public class TestLoops extends CompileAndRun {

    @Test
    public void testSimpleForLoopIncrementing () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int r (int x) { int y = 0; for (int i = 0; i < x; i++) y += i; return y; }}",
			      "r", Integer.TYPE);
	int r = (Integer)m.invoke (null, 4);
	assert r == 6 : "Unexpected return value: " + r;
    }

    @Test
    public void testSimpleForLoopDecrementing () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int r (int x) { int y = 0; for (int i = x; i >= 0; i--) y += i; return y; }}",
			      "r", Integer.TYPE);
	int r = (Integer)m.invoke (null, 4);
	assert r == 10 : "Unexpected return value: " + r;
    }

    @Test
    public void testEnhancedForLoopArray () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (int[] x) { int r = 0; for (int a : x) { r += a; } return r; }}",
			      "a", int[].class);
	int[] data = { 1, 2, 3, 4, 5};
	int r = (Integer)m.invoke (null, data);
	assert r == 15 : "Unexpected return value: " + r;
    }

    @Test
    public void testEnhancedForLoopIterator () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (java.util.List<Object> ls) { int r = 0; for (Object o : ls) { r++; } return r; }}",
			      "a", List.class);
	List<Object> data = List.of (1, 2, 3, 4, 5);
	int r = (Integer)m.invoke (null, data);
	assert r == data.size () : "Unexpected return value: " + r;
    }

    @Test
    public void testWhile () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (int r) { int ret = 0; while (r > 0) { ret += r; r--; } return ret; }}",
			      "a", Integer.TYPE);
	validateStaticIntIntMethodResult (m, -1, 0);
	validateStaticIntIntMethodResult (m, 0, 0);
	validateStaticIntIntMethodResult (m, 1, 1);
	validateStaticIntIntMethodResult (m, 2, 3);
    }

    @Test
    public void testDoWhile () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (int r) { int ret = 0; do { ret += r; r--; } while (r > 0); return ret; }}",
			      "a", Integer.TYPE);
	validateStaticIntIntMethodResult (m, -1, -1);
	validateStaticIntIntMethodResult (m, 0, 0);
	validateStaticIntIntMethodResult (m, 1, 1);
	validateStaticIntIntMethodResult (m, 2, 3);
    }

    @Test
    public void testBreakInLoop () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int foo (int max) { int sum = 0; for (int i = 0; i < 100; i++) { " +
			      "if (i == max) break; sum += i; } return sum; }}", "foo", Integer.TYPE);
	validateStaticIntIntMethodResult (m, 1, 0);
	validateStaticIntIntMethodResult (m, 4, 6);
    }

    @Test
    public void testBreakOuter () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int foo (int max) { int sum = 0; " +
			      "outer: for (int x = 1; x < 10; x++) { for (int y = 1; y < 10; y++) {" +
			      "if (x == max) break outer; sum += x * y; }} return sum; }}", "foo", Integer.TYPE);
	validateStaticIntIntMethodResult (m, 1, 0);
	validateStaticIntIntMethodResult (m, 4, 270);
    }

    private void validateStaticIntIntMethodResult (Method m, int argument, int expectedResult) throws ReflectiveOperationException {
	int r = (Integer)m.invoke (null, argument);
	assert r == expectedResult : "Got wrong result back: argument: " + argument + ", expectedResult: " + expectedResult + ", actual result: " + r;
    }
}
