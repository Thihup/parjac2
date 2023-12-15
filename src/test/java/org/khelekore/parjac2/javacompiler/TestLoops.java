package org.khelekore.parjac2.javacompiler;

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

}
