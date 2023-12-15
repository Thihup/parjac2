package org.khelekore.parjac2.javacompiler;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestAutoBox extends CompileAndRun {

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
}
