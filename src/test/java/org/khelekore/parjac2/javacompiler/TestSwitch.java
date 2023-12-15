package org.khelekore.parjac2.javacompiler;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestSwitch extends CompileAndRun {

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

    @Test
    public void testSwitchStatementSwitchRules () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int a (int s) {int r = 0; switch (s) { " +
			      "    case 1 -> r = 3; case 2 -> r = 7; default -> r = 43; } return r; }}",
			      "a", Integer.TYPE);
	int r = (Integer)m.invoke (null, 1);
	assert r == 3;
	r = (Integer)m.invoke (null, 2);
	assert r == 7;
	r = (Integer)m.invoke (null, 2232312);
	assert r == 43;
    }

    @Test
    public void testSwitchStatementSwitchBlock () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int a (int s) {int r = 0; switch (s) { " +
			      "case 1: r = 3; break; case 2: r = 7; break; default: r = 43; } return r; }}",
			      "a", Integer.TYPE);
	int r = (Integer)m.invoke (null, 1);
	assert r == 3;
	r = (Integer)m.invoke (null, 2);
	assert r == 7;
	r = (Integer)m.invoke (null, 2232312);
	assert r == 43;
    }

    @Test
    public void testSwitchStatementFallThrough () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int a (int s) {int r = 0; switch (s) { " +
			      "case 1: r = 3; break; case 2: r = 7; case 3: r = 8; break; default: r = 43; } return r; }}",
			      "a", Integer.TYPE);
	int r = (Integer)m.invoke (null, 1);
	assert r == 3;
	r = (Integer)m.invoke (null, 2);  // we fall through to 3
	assert r == 8;
	r = (Integer)m.invoke (null, 3);
	assert r == 8;
	r = (Integer)m.invoke (null, 2232312);
	assert r == 43;
    }
}
