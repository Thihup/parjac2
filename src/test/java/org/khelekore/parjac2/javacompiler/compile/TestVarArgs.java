package org.khelekore.parjac2.javacompiler.compile;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestVarArgs extends CompileAndRun {
    @Test
    public void testVarArgPassOn () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { static String v (String... va) { return va[0]; } public static String r (String... va) { return v (va); }}",
			      "r", String[].class);
	String[] data = new String[] {"a", "b"};
	Object[] args = {data};
	String s = (String)m.invoke (null, args);
	assert s.equals ("a");
    }

    @Test
    public void testVarArgConstructionSingleArgument () throws ReflectiveOperationException {
	Method m = getMethod ("C",
			      "public class C {" +
			      "    static String v (String... va) { return va[0]; } " +
			      "    public static String r (String s1) { return v (s1); }}",
			      "r", String.class);
	String s = (String)m.invoke (null, "a");
	assert s.equals ("a");
    }

    @Test
    public void testVarArgConstructionMultiArgument () throws ReflectiveOperationException {
	Method m = getMethod ("C",
			      "public class C {" +
			      "    static String v (String... va) { return va[0]; } " +
			      "    public static String r (String s1, String s2) { return v (s1, s2); }}",
			      "r", String.class, String.class);
	String s = (String)m.invoke (null, "a", "b");
	assert s.equals ("a");
    }
}
