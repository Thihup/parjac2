package org.khelekore.parjac2.javacompiler.compile;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestStringConcat extends CompileAndRun {

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
}
