package org.khelekore.parjac2.javacompiler.compile;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.testng.annotations.Test;

public class TestTryCatchFinally extends CompileAndRun {
    @Test
    public void testTryFinally () throws ReflectiveOperationException {
	Method m = getMethod ("C", "import java.io.*; class C { public static int a (boolean b) throws IOException { " +
			      "int x = 7; try { if (b) throw new IOException (); } finally { x++; } return x;}}",
			      "a", Boolean.TYPE);
	int r = (Integer)m.invoke (null, false);
	assert r == 8;
	try {
	    m.invoke (null, true);
	} catch (InvocationTargetException ite) {
	    Throwable cause = ite.getCause ();
	    assert cause instanceof IOException;
	}
    }

    /* TODO: this crashes during bytecode generation:
     * TODO: it tries to inline the finally after the throws (which is wrong) and adds a jump to position outside of method (also wrong)
     */
    /*
    @Test
    public void testTryFinallyTryAlwaysThrows () throws ReflectiveOperationException {
	Method m = getMethod ("C", "import java.io.*; class C { public static int a (boolean b) throws IOException {" +
			      "int x = 7; try { if (b) throw new IOException (); else throw new RuntimeException (); } finally { x++; }}}",
			      "a", Boolean.TYPE);
    }
    */
}
