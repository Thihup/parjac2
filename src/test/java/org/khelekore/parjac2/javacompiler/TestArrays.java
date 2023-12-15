package org.khelekore.parjac2.javacompiler;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestArrays extends CompileAndRun {

    @Test
    public void testNewIntArray () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int[] a (int s) {return new int[s]; }}",
			      "a", Integer.TYPE);
	int[] arr = (int[])m.invoke (null, 37);
	assert arr.length == 37 : "Wrong size of array: " + arr.length;
    }

    @Test
    public void testNewDoubleArray () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static double[] a (int s) {return new double[s]; }}",
			      "a", Integer.TYPE);
	double[] arr = (double[])m.invoke (null, 37);
	assert arr.length == 37 : "Wrong size of array: " + arr.length;
    }

    @Test
    public void testNewStringArray () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static String[] a (int s) {return new String[s]; }}",
			      "a", Integer.TYPE);
	String[] arr = (String[])m.invoke (null, 37);
	assert arr.length == 37 : "Wrong size of array: " + arr.length;
    }

    @Test
    public void testNewMultiIntArray () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int[][][] a (int s) {return new int[s][5][]; }}",
			      "a", Integer.TYPE);
	int[][][] arr = (int[][][])m.invoke (null, 37);
	assert arr.length == 37 : "Wrong size of array: " + arr.length;
    }

    @Test
    public void testGetArrayElement () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (int[] s, int pos) {return s[pos]; }}",
			      "a", int[].class, Integer.TYPE);
	int[] data = {1, 2, 3, 4};
	int r = (Integer)m.invoke (null, data, 0);
	assert r == 1 : "Wrong element returned: " + r;
	r = (Integer)m.invoke (null, data, 2);
	assert r == 3 : "Wrong element returned: " + r;
    }

    @Test
    public void testGetArrayArray () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int[] a (int[][] s, int pos) {return s[pos]; }}",
			      "a", int[][].class, Integer.TYPE);
	int[][] data = new int[3][4];
	int[] r = (int[])m.invoke (null, data, 0);
	assert r != null : "Got null back";
    }

    @Test
    public void testGetMultiArrayElement () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (int[][] s, int x, int y) {return s[x][y]; }}",
			      "a", int[][].class, Integer.TYPE, Integer.TYPE);
	int[][] data = {{1, 2, 3}, {7, 5, 3}};
	int r = (Integer)m.invoke (null, data, 1, 1);
	assert r == 5 : "Wrong element returned: " + r;
    }
}
