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

    @Test
    public void testCatchHandling () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static void f (Runnable r, Runnable iaec) { " +
			      "try { r.run (); } catch (IllegalArgumentException e) { iaec.run (); }}}",
			      "f", Runnable.class, Runnable.class);
	Runnable r1 = () -> {/* empty */};
	Runnable r2 = () -> { throw new IllegalArgumentException (); };
	DidItRun ec = new DidItRun ();
	m.invoke (null, r1, ec);
	assert !ec.gotIt;
	m.invoke (null, r2, ec);
	assert ec.gotIt;
    }

    @Test
    public void testCatchFinallyHandling () throws ReflectiveOperationException {
	Method m = getMethod ("C", "public class C { public static void f (Runnable r, Runnable iaec, Runnable finallyRunner) { " +
			      "try { r.run (); } catch (IllegalArgumentException e) { iaec.run (); } finally { finallyRunner.run (); }}}",
			      "f", Runnable.class, Runnable.class, Runnable.class);
	Runnable r1 = () -> {/* empty */};
	Runnable r2 = () -> { throw new IllegalArgumentException (); };
	DidItRun exceptionHandler = new DidItRun ();
	DidItRun finallyHandler = new DidItRun ();

	m.invoke (null, r1, exceptionHandler, finallyHandler);
	assert !exceptionHandler.gotIt;
	assert finallyHandler.gotIt;
	finallyHandler = new DidItRun ();
	m.invoke (null, r2, exceptionHandler, finallyHandler);
	assert exceptionHandler.gotIt;
	assert finallyHandler.gotIt;
    }

    private static class TestAutoCloseable implements AutoCloseable {
	public boolean closed = false;

	@Override public void close () {
	    closed = true;
	}
    }

    private static class DidItRun implements Runnable {
	boolean gotIt = false;

	@Override public void run () {
	    gotIt = true;
	}
    }
}
