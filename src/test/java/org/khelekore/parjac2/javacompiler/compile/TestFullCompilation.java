package org.khelekore.parjac2.javacompiler.compile;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

import org.testng.annotations.Test;

public class TestFullCompilation extends CompileAndRun {

    @Test
    public void testReturnZero () throws ReflectiveOperationException {
	testStaticReturnIntValue (0);
    }

    @Test
    public void testReturnMinusOne () throws ReflectiveOperationException {
	testStaticReturnIntValue (-1);
    }

    @Test
    public void testReturnMinusTwo () throws ReflectiveOperationException {
	testStaticReturnIntValue (-2);
    }

    @Test
    public void testReturnMinusMax () throws ReflectiveOperationException {
	testStaticReturnIntValue (Integer.MIN_VALUE);
    }

    @Test
    public void testReturnMinusMaxPlusOne () throws ReflectiveOperationException {
	testStaticReturnIntValue (Integer.MIN_VALUE + 1);
    }

    @Test
    public void testReturnFive () throws ReflectiveOperationException {
	testStaticReturnIntValue (5);
    }

    @Test
    public void testReturnMaxValue () throws ReflectiveOperationException {
	testStaticReturnIntValue (Integer.MAX_VALUE);
    }

    private void testStaticReturnIntValue (int x) throws ReflectiveOperationException {
	int r = (Integer)getReturnFromStaticMethod ("C", "class C { public static int r () { return " + x + "; }}");
	assert r == x : "expected " + x + ", but got: " + r;
    }

    @Test
    public void testReturnDoubleValue () throws ReflectiveOperationException {
	double x = 3.14;
	double r = (Double)getReturnFromStaticMethod ("C", "class C { public static double r () { return " + x + "; }}");
	assert r == x : "expected " + x + ", but got: " + r;
    }

    @Test
    private void testStaticReturnObject () throws ReflectiveOperationException {
	Object r = getReturnFromStaticMethod ("C", "class C { public static Object r () { return null; }}");
	assert r == null : "expected null, but got: " + r;
    }

    @Test
    private void testStaticReturnVoidNoReturn () throws ReflectiveOperationException {
	Object r = getReturnFromStaticMethod ("C", "class C { public static void r () { }}");
	assert r == null : "expected null, but got: " + r;
    }

    @Test
    private void testStaticReturnVoidWithReturn () throws ReflectiveOperationException {
	Object r = getReturnFromStaticMethod ("C", "class C { public static void r () { return; }}");
	assert r == null : "expected null, but got: " + r;
    }

    public Object getReturnFromStaticMethod (String className, String classText) throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass (className, classText);
	Method m = c.getMethod ("r");
	m.setAccessible (true);
	return m.invoke (null);
    }

    @Test
    public void testFloatArray () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { public float[] fs = {7.2F, 3.9F}; }");
	Object o = c.getConstructor ().newInstance ();
	Field f = c.getField ("fs");
	Object fs = f.get (o);
	assert fs.getClass ().isArray ();
	Object v = Array.get (fs, 0);
	float actual = (Float)v;
	assert actual == 7.2F;
    }

    @Test
    public void testReturnConditional () throws ReflectiveOperationException {
	testReturnConditional ("C", "class C { public static boolean isZero (int x) { return x == 0; }}", "isZero");
	testReturnConditional ("C", "class C { public static boolean isZero (int x) { return 0 == x; }}", "isZero");
    }

    private void testReturnConditional (String className, String text, String methodName) throws ReflectiveOperationException {
	Method m = getMethod (className, text, methodName, Integer.TYPE);
	boolean r1 = (Boolean)m.invoke (null, 0);
	assert r1 == true : "expected true for 0";
	boolean r2 = (Boolean)m.invoke (null, 1);
	assert r2 == false : "expected false for non-zero";
    }

    @Test
    private void testExtraEmptyStatements () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r () { int x = 17;;;; return x; }}", "r");
	int r = (Integer)m.invoke (null);
	assert r == 17;
    }

    @Test
    public void testIntAddition () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (int x, int y) { return x + y; }}",
			      "r", Integer.TYPE, Integer.TYPE);
	int x = 2;
	int y = 7;
	int r = (Integer)m.invoke (null, x, y);
	int expected = x + y;
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r;
    }

    @Test
    public void testDoubleAddition () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static double r (double x, double y) { return x + y; }}",
			      "r", Double.TYPE, Double.TYPE);
	double x = 2.5;
	double y = 7.9;
	double r = (Double)m.invoke (null, x, y);
	double expected = x + y;
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r;
    }

    @Test
    public void testDoubleIntAddition () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static double r (double x, int y) { return x + y; }}",
			      "r", Double.TYPE, Integer.TYPE);
	double x = 2.4;
	int y = 9;
	double r = (Double)m.invoke (null, x, y);
	double expected = x + y;
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r;
    }

    @Test
    public void testIntDoubleAddition () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static double r (int x, double y) { return x + y; }}",
			      "r", Integer.TYPE, Double.TYPE);
	int x = 2;
	double y = 9.91;
	double r = (Double)m.invoke (null, x, y);
	double expected = x + y;
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r;
    }

    @Test
    public void testFloatLongAddtion () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static double r (float x, long y) { return x + y; }}",
			      "r", Float.TYPE, Long.TYPE);
	float x = 2.1F;
	long y = 3L;
	double r = (Double)m.invoke (null, x, y);
	double expected = x + y;
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r;
    }

    @Test
    public void testIntMaths () throws ReflectiveOperationException {
	testIntMath ("x * y", 2, 3, 6);
	testIntMath ("x / y", 7, 3, 2);
	testIntMath ("x % y", 7, 3, 1);
    }

    @Test
    public void testShifs () throws ReflectiveOperationException {
	testIntMath ("x << y", 0x8080, 1, 0x10100);
	testIntMath ("x >> y", 0x8080, 1, 0x4040);
	testIntMath ("x >>> y", 0xffffffff, 16, 0xffff);
    }

    @Test
    public void testBitOperations () throws ReflectiveOperationException {
	testIntMath ("x & y", 0x70707, 0x404, 0x404);
	testIntMath ("x | y", 0x70707, 0x78, 0x7077f);
	testIntMath ("x ^ y", 0x70707, 0x72, 0x70775);
    }

    private void testIntMath (String expression, int x, int y, int expected) throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (int x, int y) { return " + expression + "; }}",
			      "r", Integer.TYPE, Integer.TYPE);
	int r = (Integer)m.invoke (null, x, y);
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r + ", expression: " + expression;
    }

    @Test
    public void testUnaryIntOperations () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (int x) { return ~x; }}", "r", Integer.TYPE);
	int value = 0xff00ff00;
	int expected = ~value;
	int r = (Integer)m.invoke (null, value);
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r;
    }

    @Test
    public void testRelational () throws ReflectiveOperationException {
	testRelational ("x < y", 3, 7, true);
	testRelational ("x < y", 7, 3, false);

	testRelational ("x > y", 3, 7, false);
	testRelational ("x > y", 7, 3, true);

	testRelational ("x <= y", 3, 7, true);
	testRelational ("x <= y", 3, 3, true);
	testRelational ("x <= y", 7, 3, false);

	testRelational ("x > y", 3, 7, false);
	testRelational ("x >= y", 3, 3, true);
	testRelational ("x > y", 7, 3, true);
    }

    private void testRelational (String expression, int x, int y, boolean expected) throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static boolean r (int x, int y) { return " + expression + "; }}",
			      "r", Integer.TYPE, Integer.TYPE);
	boolean r = (Boolean)m.invoke (null, x, y);
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r + ", expression: " + expression;
    }

    @Test
    public void testBooleanOperators () throws ReflectiveOperationException {
	testLogical ("x && y", false, false, false);
	testLogical ("x && y", true, false, false);
	testLogical ("x && y", false, true, false);
	testLogical ("x && y", true, true, true);

	testLogical ("x || y", false, false, false);
	testLogical ("x || y", true, false, true);
	testLogical ("x || y", false, true, true);
	testLogical ("x || y", true, true, true);
    }

    private void testLogical (String expression, boolean x, boolean y, boolean expected) throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static boolean r (boolean x, boolean y) { return " + expression + "; }}",
			      "r", Boolean.TYPE, Boolean.TYPE);
	boolean r = (Boolean)m.invoke (null, x, y);
	assert r == expected : "Got wrong value back, expected: " + expected + ", but got: " + r + ", expression: " + expression;
    }

    @Test
    public void testUnaryNegation () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static boolean r (boolean x) { return !x; }}", "r", Boolean.TYPE);
	boolean r = (Boolean)m.invoke (null, true);
	assert r == false : "Got wrong value back";
	r = (Boolean)m.invoke (null, false);
	assert r == true : "Got wrong value back";
    }

    @Test
    public void testConstructorTakingMultiple () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("D", "public class D { public D (int x, int y, int z) { }}");
	Constructor<?> ctr = c.getConstructor (Integer.TYPE, Integer.TYPE, Integer.TYPE);
	int x = 3;
	Object o = ctr.newInstance (x, x, x);
	assert o != null : "Expected an instance";
    }

    @Test
    public void testConstructorAndGetter () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { private int x; public C (int x) { this.x = x; } public int x () { return x; }}");
	Constructor<?> ctr = c.getConstructor (Integer.TYPE);
	int x = 3;
	Object o = ctr.newInstance (x);
	Method m = c.getMethod ("x");
	int r = (Integer)m.invoke (o);
	assert r == x : "Expected to get the same value back, not: " + r;
    }

    @Test
    public void testPostIncrementParamenter () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int r (int x) { x++; return x; }}",
			      "r", Integer.TYPE);
	int r = (Integer)m.invoke (null, 7);
	assert r == 8 : "Increment returned wrong value: " + r;
    }

    @Test
    public void testPostIncrementStaticField () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static int x = 17; public static int r () { x++; return x; }}", "r");
	int r = (Integer)m.invoke (null);
	assert r == 18 : "Increment returned wrong value: " + r;
    }

    @Test
    public void testPostIncrementArraySlot () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static int[] x = {17, 3, 7, 19, 23};" +
			      "public static int r () { x[4]++; return x[4]; }}", "r");
	int r = (Integer)m.invoke (null);
	assert r == 24 : "Increment returned wrong value: " + r;
    }

    @Test
    public void testValueFromPostChangeIsHandled () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a () { int r = 3; int x = r--; return x;  }}", "a");
	int r = (Integer)m.invoke (null);
	assert r == 3 : "Value from post decrement operation should be value from before change, but got: " + r;
    }

    @Test
    public void testPostIncrementThisField () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { int x = 17; public int r () { this.x++; return this.x; }}");
	Object o = c.getConstructor ().newInstance ();
	Method m = c.getMethod ("r");
	int r = (Integer)m.invoke (o);
	assert r == 18 : "Increment returned wrong value: " + r;
    }

    @Test
    public void testPostIncrementField () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { int x = 17; public int r () { x++; return x; }}");
	Constructor<?> ctr = c.getConstructor ();
	Object o = ctr.newInstance ();
	Method m = c.getMethod ("r");
	int r = (Integer)m.invoke (o);
	assert r == 18 : "Increment returned wrong value: " + r;
    }

    @Test
    public void testPreIncrementDouble () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static double a (double d) { return ++d; }}", "a", Double.TYPE);
	double r = (Double)m.invoke (null, 1.0);
	assert r == 2.0;
	r = (Double)m.invoke (null, 1.7);
	assert r == 2.7;
    }

    @Test
    public void testPostIncrementDouble () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static double a (double d) { d++; return d; }}", "a", Double.TYPE);
	double r = (Double)m.invoke (null, 1.0);
	assert r == 2.0;
	r = (Double)m.invoke (null, 1.7);
	assert r == 2.7;
    }

    @Test
    public void testPostIncrementShort () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static short a (short d) { d++; return d; }}", "a", Short.TYPE);
	short r = (Short)m.invoke (null, (short)1);
	assert r == 2;
	r = (Short)m.invoke (null, (short)67);
	assert r == 68;
    }

    @Test
    public void testPostIncrementDoubleField () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static double d = 7.2; public static double a () { double r = d++; return r; }}", "a");
	double r = (Double)m.invoke (null);
	assert r == 7.2;
    }

    @Test
    public void testPreIncrementDoubleField () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static double d = 7.2; public static double a () { double r = ++d; return r; }}", "a");
	double r = (Double)m.invoke (null);
	assert r == 8.2;
    }

    @Test
    public void testPreIncrementDoubleInstanceField () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { public double d = 7.2; public double a () { double r = ++d; return r; }}");
	Object instance = c.getConstructor ().newInstance ();
	Method m = c.getMethod ("a");
	m.setAccessible (true);
	double r = (Double)m.invoke (instance);
	assert r == 8.2;
    }

    @Test
    public void testPutStaticFieldInOtherClass () throws ReflectiveOperationException {
	String code = "class A { public static int z = 1; } class B { public static void a () { A.z *= 3; }}";
	Map<String, Class<?>> classes = compileAndGetClasses (code);
	Class<?> aClass = classes.get ("A");
        Field aField = aClass.getField ("z");
	aField.setAccessible (true);
        int r = (Integer)aField.get (null);
	assert r == 1;

	Class<?> bClass = classes.get ("B");
        Method m = bClass.getMethod ("a");
	m.setAccessible (true);
	m.invoke (null);
        r = (Integer)aField.get (null);
	assert r == 3;
    }

    @Test
    public void testPutInstanceFieldInOtherClass () throws ReflectiveOperationException {
	Class<?> b = compileAndGetClass ("B", "class A { public int z = 1; } " +
					 "public class B { private A a = new A (); public int a () { a.z *= 3; return a.z; }}");
	Object o = b.getConstructor ().newInstance ();
        Method m = b.getMethod ("a");
	m.setAccessible (true);
	int r = (Integer)m.invoke (o);
	assert r == 3;
    }

    @Test
    public void testInstanceCreation () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static public Object r () { return new Object (); }}", "r");
	Object r = m.invoke (null);
	assert r != null : " expected to get an object back, but got null";
    }

    @Test
    public void testLambdaCall () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { " +
					 "public boolean b = false; " +
					 "private void a (Runnable r) { r.run (); } " +
					 "public void b () { a(() -> b = true); }}");
        checkDynamicMethod (c, "b", "b");
    }

    @Test
    public void testStaticLambdaCall () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { " +
					 "public static boolean b = false; " +
					 "private static void a (Runnable r) { r.run (); } " +
					 "public static void b () { a(() -> b = true); }}");
        checkDynamicMethod (c, "b", "b", null);
    }

    @Test
    public void testLambdaAssignment () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C {public boolean b; public void c () { Runnable r = () -> b = true; r.run ();}}");
	checkDynamicMethod (c, "c", "b");
    }

    @Test
    public void testMethodReferenceAssignment () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C {public boolean b; void a () {b = true;} public void c () { Runnable r = this::a; r.run ();}}");
	checkDynamicMethod (c, "c", "b");
    }

    @Test
    public void testCallingStaticMethodReferenceFromInstance () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C",
					 "public class C {public static boolean b; static void a () {b = true;}" +
					 "    public void c () { Runnable r = C::a; r.run ();}}");
	checkDynamicMethod (c, "c", "b");
    }

    private void checkDynamicMethod (Class<?> c, String methodName, String fieldName) throws ReflectiveOperationException {
	Object o = c.getConstructor ().newInstance ();
	checkDynamicMethod (c, methodName, fieldName, o);
    }

    private void checkDynamicMethod (Class<?> c, String methodName, String fieldName, Object runOn) throws ReflectiveOperationException {
        Method m = c.getMethod (methodName);
        Field f = c.getField (fieldName);
        assert f.get (runOn) == Boolean.FALSE;
	m.invoke (runOn);
	assert f.get (runOn) == Boolean.TRUE;
    }

    @Test
    public void testInstanceof () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static boolean a (Object o) {return o instanceof String; }}",
			      "a", Object.class);
	boolean b = (Boolean)m.invoke (null, (Object)null);
	assert !b;
	b = (Boolean)m.invoke (null, new Object ());
	assert !b;
	b = (Boolean)m.invoke (null, "wow");
	assert b;
    }

    @Test
    public void testInstanceofInIf () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (Object o) {if (o instanceof String) return 3; return 4; }}",
			      "a", Object.class);
	int r = (Integer)m.invoke (null, new Object ());
	assert r == 4;
	r = (Integer)m.invoke (null, "wow");
	assert r == 3;
    }

    @Test
    public void testComplexInstanceofInIf () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (Object o, boolean b) {if (o instanceof String s && b) return 3; return 4; }}",
			      "a", Object.class, Boolean.TYPE);
	int r = (Integer)m.invoke (null, new Object (), Boolean.TRUE);
	assert r == 4;
	r = (Integer)m.invoke (null, "wow", Boolean.TRUE);
	assert r == 3;
    }

    @Test
    public void testInstanceofVariable () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static int a (Object o) {if (o instanceof String s && s != \"a\") return 3; return 4; }}",
			      "a", Object.class);
	int r = (Integer)m.invoke (null, new Object ());
	assert r == 4;
	r = (Integer)m.invoke (null, "wow");
	assert r == 3;
    }

    @Test
    public void testRecordToString () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("R", "public record R (int x, int y) {}");
	Object o = c.getConstructor (Integer.TYPE, Integer.TYPE).newInstance (6, 4);
	Method m = c.getMethod ("toString");
	Object r = m.invoke (o);
	assert r.equals ("R[x=6, y=4]") : "Got wrong string back: " + r;
    }

    @Test
    public void testVarArgCall () throws ReflectiveOperationException {
	testIntArrayHandling ("C", "class C { public static int a (int... is) { if (is == null) return 0; return is.length; }}", "a");
    }

    @Test
    public void testArrayLength () throws ReflectiveOperationException {
	testIntArrayHandling ("C", "class C { public static int a (int[] is) { if (is == null) return 0; return is.length; }}", "a");
    }

    private void testIntArrayHandling (String cls, String text, String method) throws ReflectiveOperationException {
	Method m = getMethod (cls, text, method, int[].class);
	int r = (Integer)m.invoke (null, (int[])null);
	assert r == 0;
	r = (Integer)m.invoke (null, new int[] {1, 2});
	assert r == 2;
    }

    @Test
    public void testStackPopFromMethodCall () throws ReflectiveOperationException {
	// having "int y = x (); x ();" does not trigger any problem, but with the if we do.
	Method m = getMethod ("C", "class C { static int x () { return 43; } public static void a () { int y = x (); if (y > 3) x (); }}", "a");
	m.invoke (null);
    }

    @Test
    public void testStackPop2OnLongFromMethodCall () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { static int x () { return 43; } public static void a () { if (x() > 3) System.currentTimeMillis(); }}", "a");
	m.invoke (null);
    }

    @Test
    public void testStackPopNotDoneOnChainedMethod () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { static String x () { return \"foo\"; } public static void a () { int l = x ().length (); }}", "a");
	m.invoke (null);
    }

    @Test
    public void testEnumValues () throws ReflectiveOperationException {
	Method m = getMethod ("E", "public enum E { Y, N }", "values");
	Object o = m.invoke (null);
	assert o != null;
	assert o.getClass ().isArray ();
	int expectedLength = 2;
	assert Array.getLength (o) == expectedLength;
	for (int i = 0; i < expectedLength; i++) {
	    Object e = Array.get (o, i);
	    assert e != null;
	    assert e.getClass ().isEnum ();
	}
    }

    @Test(expectedExceptions = ReflectiveOperationException.class)
    public void testBasicThrows () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static void a () { throw new RuntimeException(); }}", "a");
	m.invoke (null);
    }
}
