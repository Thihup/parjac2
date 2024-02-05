package org.khelekore.parjac2.javacompiler.compile;

import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestTryResources extends CompileAndRun {
    @Test
    public void testSimpleForLoopIncrementing () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static void f (AutoCloseable a) throws Exception { try (a) { int i = 2; i++; }}}",
			      "f", AutoCloseable.class);
	TestAutoCloseable e = new TestAutoCloseable ();
	m.invoke (null, e);
	assert e.closed : "Did not run close()";
    }

    private static class TestAutoCloseable implements AutoCloseable {
	public boolean closed = false;

	@Override public void close () {
	    closed = true;
	}
    }
}
