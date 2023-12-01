package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Collection;
import java.util.Map;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestFullCompilation {

    private Grammar grammar;
    private Rule goalRule;
    private JavaTokens javaTokens;
    private InMemorySourceProvider sourceProvider;
    private InMemoryBytecodeWriter bytecodeWriter;
    private CompilationArguments settings;
    private CompilerDiagnosticCollector diagnostics;
    private ClassInformationProvider cip;

    @BeforeClass
    public void createTools () throws IOException {
	grammar = new Grammar ();
	goalRule = JavaGrammarHelper.readAndValidateRules (grammar, false);
	javaTokens = new JavaTokens (grammar);
	sourceProvider = new InMemorySourceProvider ();
	bytecodeWriter = new InMemoryBytecodeWriter ();
	settings = new CompilationArguments (sourceProvider, bytecodeWriter, null, false, false);
    }

    @BeforeMethod
    public void createDiagnostics () {
	// start with a clean slate every time
	diagnostics = new CompilerDiagnosticCollector ();
	sourceProvider.clean ();
	bytecodeWriter.clean ();

	cip = new ClassInformationProvider (diagnostics, settings);
	cip.scanClassPath ();
    }

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

    private void testSimpleTrueFalse (Method m, int trueVal, int falseVal) throws ReflectiveOperationException {
	int r = (Integer)m.invoke (null, true, trueVal, falseVal);
	assert r == trueVal : "Got wrong number back";
	r = (Integer)m.invoke (null, false, trueVal, falseVal);
	assert r == falseVal : "Got wrong number back";
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
    public void testPostIncrementThisField () throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass ("C", "public class C { int x = 17; public int r () { this.x++; return this.x; }}");
	Object o = c.getConstructor ().newInstance ();;
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
    public void testPutFieldInOtherClass () throws ReflectiveOperationException {
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
    public void testInstanceCreation () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static public Object r () { return new Object (); }}", "r");
	Object r = m.invoke (null);
	assert r != null : " expected to get an object back, but got null";
    }

    @Test
    public void testAutoBoxReturnNumber () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static public Number r () { return 4; }}", "r");
	Object r = m.invoke (null);
	assert r != null : " expected to get an object back, but got null";
	assert r.equals (4) : " expected to get correct value back";
    }

    @Test
    public void testAutoBoxReturnLong () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static Long r () { return 4L; }}", "r");
	Object r = m.invoke (null);
	assert r != null : " expected to get an object back, but got null";
	assert r.equals (4L) : " expected to get correct value back";
    }

    @Test
    public void testAutoBoxLongArgument () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static void a (Long l) {} public static void b () { a(4L); }}", "b");
	m.invoke (null);
    }

    @Test
    public void testAutoUnBoxReturnLong () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static long r () { Long l = 77L; return l; }}", "r");
	m.invoke (null);
    }

    @Test
    public void testAutoUnBoxLongArgument () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { static void a (long l) {} public static void r () { Long l = 77L; a(l); }}", "r");
	m.invoke (null);
    }

    @Test
    public void testAutoWidenReturn () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static long r () { return 4; }}", "r");
	long l = (Long)m.invoke (null);
	assert l == 4L : "Got wrong number back: " + l;
    }

    @Test
    public void testAutoWidenArgument () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { static void a (long l) {} public static void b () { a(4); }}", "b");
	m.invoke (null);
    }

    @Test
    public void testStringConcat () throws ReflectiveOperationException {
	testSimpleStringConcat ("\"a\" + i", 37, "a37");
	testSimpleStringConcat ("i + \"a\"", 37, "37a");
	testSimpleStringConcat ("\"a\" + i", 37.5, "a37.5");
	testSimpleStringConcat ("i + \"a\"", 37.5, "37.5a");
    }

    @Test
    public void testPlusAndStringConcat () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static String a (int i) {return i + 4 + \"!\"; }}",
			      "a", Integer.TYPE);
	Object r = m.invoke (null, 37);
	assert "41!".equals (r) : "Got wrong result from int + int + String concat: " + r;
    }

    @Test
    public void testStringConcatWithSeveralInts () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static String a (int i) {return \"!\" + i + 4; }}",
			      "a", Integer.TYPE);
	Object r = m.invoke (null, 37);
	assert "!374".equals (r) : "Got wrong result from int + int + String concat: " + r;
    }

    @Test
    public void testStringConcatWithSeveralIntsInParenthesis () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static String a (int i) {return \"!\" + (i + 4); }}",
			      "a", Integer.TYPE);
	Object r = m.invoke (null, 37);
	assert "!41".equals (r) : "Got wrong result from int + int + String concat: " + r;
    }

    private void testSimpleStringConcat (String code, int iValue, String expected) throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static String a (int i) {return " + code + "; }}",
			      "a", Integer.TYPE);
	String r = (String)m.invoke (null, iValue);
	assert r.equals (expected) : "Got wrong value back: " + r;
    }

    private void testSimpleStringConcat (String code, double iValue, String expected) throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static String a (double i) {return " + code + "; }}",
			      "a", Double.TYPE);
	String r = (String)m.invoke (null, iValue);
	assert r.equals (expected) : "Got wrong value back: " + r;
    }

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
    public void testSwitchExpression () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static Object a (String s) {Object o = switch (s) { " +
			      "    case \"foo\" -> 1; case \"bar\" -> \"what\"; default -> null; }; return o; }}",
			      "a", String.class);
	Object o = m.invoke (null, "foo");
	assert Integer.valueOf (1).equals (o);
	o = m.invoke (null, "bar");
	assert "what".equals (o);
	o = m.invoke (null, "whatever");
	assert o == null;
    }

    private Method getMethod (String className, String text, String methodName, Class<?> ... types) throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass (className, text);
	Method m = c.getMethod (methodName, types);
	m.setAccessible (true);
	return m;
    }

    private Class<?> compileAndGetClass (String className, String input) throws ClassNotFoundException {
	Map<String, Class<?>> classes = compileAndGetClasses (className + ".java", input);
	return classes.get (className);
    }

    private Map<String, Class<?>> compileAndGetClasses (String text) throws ClassNotFoundException {
	return compileAndGetClasses ("dynamic.java", text);
    }

    private Map<String, Class<?>> compileAndGetClasses (String filename, String text) throws ClassNotFoundException {
	sourceProvider.input (filename, text);
	Compiler c = new Compiler (diagnostics, grammar, javaTokens, goalRule, settings);
	c.compile ();
	assert diagnostics.errorCount () == 0 :
	String.format ("Expected no compilation errors: %s", TestParserHelper.getParseOutput (diagnostics));
	InMemoryClassLoader cl = new InMemoryClassLoader (bytecodeWriter.classes);
	return cl.loadAllClasses ();
    }

    private static class InMemorySourceProvider implements SourceProvider {
	private String filename;
	private String input;

	@Override public void setup (CompilerDiagnosticCollector diagnostics) {
	    // empty
	}

	@Override public Collection<DirAndPath> getSourcePaths () {
	    return List.of (new DirAndPath (Paths.get ("src"), Paths.get (filename)));
	}

	@Override public CharBuffer getInput (Path path) {
	    return CharBuffer.wrap (input);
	}

	public void input (String filename, String input) {
	    this.filename = filename;
	    this.input = input;
	}

	public void clean () {
	    input = null;
	    filename = null;
	}
    }

    private static class InMemoryBytecodeWriter implements BytecodeWriter {
	private Map<String, byte[]> classes = new HashMap<> ();

	@Override public void createDirectory (Path path) {
	    // ignore, everything in memory
	}

	@Override public void write (String className, Path path, byte[] data) {
	    classes.put (className, data);
	}

	public void clean () {
	    classes.clear ();
	}
    }

    private static class InMemoryClassLoader extends ClassLoader {
	private final Map<String, byte[]> classes;

	public InMemoryClassLoader (Map<String, byte[]> classes) {
	    this.classes = classes;
	}

	@Override protected Class<?> findClass (String name) throws ClassNotFoundException {
	    byte[] data = classes.get (name);
	    if (data == null)
		throw new ClassNotFoundException (name + " not found");
	    return defineClass (name, data, 0, data.length);
	}

	public Map<String, Class<?>> loadAllClasses () throws ClassNotFoundException {
	    Map<String, Class<?>> ret = new HashMap<> ();
	    for (String name : classes.keySet ())
		ret.put (name, loadClass (name));
	    return ret;
	}
    }
}
