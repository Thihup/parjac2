package org.khelekore.parjac2.javacompiler.compile;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.ClassInformationProvider;
import org.khelekore.parjac2.javacompiler.CompilationArguments;
import org.khelekore.parjac2.javacompiler.Compiler;
import org.khelekore.parjac2.javacompiler.InMemoryBytecodeWriter;
import org.khelekore.parjac2.javacompiler.InMemoryClassLoader;
import org.khelekore.parjac2.javacompiler.InMemorySourceProvider;
import org.khelekore.parjac2.javacompiler.JavaGrammarHelper;
import org.khelekore.parjac2.javacompiler.JavaTokens;
import org.khelekore.parjac2.javacompiler.TestParserHelper;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

public abstract class CompileAndRun {

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

    public Method getMethod (String className, String text, String methodName, Class<?> ... types) throws ReflectiveOperationException {
	Class<?> c = compileAndGetClass (className, text);
	Method m = c.getMethod (methodName, types);
	m.setAccessible (true);
	return m;
    }

    public Class<?> compileAndGetClass (String className, String input) throws ClassNotFoundException {
	Map<String, Class<?>> classes = compileAndGetClasses (className + ".java", input);
	return classes.get (className);
    }

    public Map<String, Class<?>> compileAndGetClasses (String text) throws ClassNotFoundException {
	return compileAndGetClasses ("dynamic.java", text);
    }

    public Map<String, Class<?>> compileAndGetClasses (String filename, String text) throws ClassNotFoundException {
	Map<String, byte[]> classes = compileAndGetBytecode (filename, text);
	InMemoryClassLoader cl = new InMemoryClassLoader (classes);
	return cl.loadAllClasses ();
    }

    public Map<String, byte[]> compileAndGetBytecode (String filename, String text) {
	sourceProvider.input (filename, text);
	Compiler c = new Compiler (diagnostics, grammar, javaTokens, goalRule, settings);
	c.compile ();
	assert diagnostics.errorCount () == 0 :
	String.format ("Expected no compilation errors: %s", TestParserHelper.getParseOutput (diagnostics));
	return bytecodeWriter.classes ();
    }
}
