package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
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
	Class<?> c = getFirstClass (className, classText);
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
    public void testIfWithReturn () throws ReflectiveOperationException {
	Method m = getMethod ("C", "class C { public static int r (boolean b, int x, int y) { if (b) return x; else return y; }}",
			      "r", Boolean.TYPE, Integer.TYPE, Integer.TYPE);
	testSimpleTrueFalse (m, 72, 98);
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

    private Method getMethod (String className, String text, String methodName, Class<?> ... types) throws ReflectiveOperationException {
	Class<?> c = getFirstClass (className, text);
	Method m = c.getMethod (methodName, types);
	m.setAccessible (true);
	return m;
    }

    private Class<?> getFirstClass (String className, String input) throws ClassNotFoundException {
	sourceProvider.input (className + ".java", input);
	Compiler c = new Compiler (diagnostics, grammar, javaTokens, goalRule, settings);
	c.compile ();
	assert diagnostics.errorCount () == 0 : String.format ("Expected no compilation errors: %s", TestParserHelper.getParseOutput (diagnostics));
	byte[] classData = bytecodeWriter.classes.values ().iterator ().next ();
	ClassLoader cl = new InMemoryClassLoader (classData);
	return cl.loadClass (className);
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
	private Map<Path, byte[]> classes = new HashMap<> ();

	@Override public void createDirectory (Path path) {
	    // ignore, everything in memory
	}

	@Override public void write (Path path, byte[] data) {
	    classes.put (path, data);
	}

	public void clean () {
	    classes.clear ();
	}
    }

    private static class InMemoryClassLoader extends ClassLoader {
	private byte[] data;

	public InMemoryClassLoader (byte[] data) {
	    this.data = data;
	}

	@Override protected Class<?> findClass (String name) throws ClassNotFoundException {
	    return defineClass (name, data, 0, data.length);
	}
    }
}
