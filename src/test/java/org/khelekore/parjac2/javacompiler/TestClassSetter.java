package org.khelekore.parjac2.javacompiler;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.DottedName;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExpressionStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodInvocation;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestClassSetter {

    private Grammar grammar;
    private JavaTokens javaTokens;
    private CompilationArguments settings;
    private CompilerDiagnosticCollector diagnostics;
    private ClassInformationProvider cip;

    @BeforeClass
    public void createTools () {
	grammar = TestParserHelper.getJavaGrammarFromFile ("CompilationUnit", false);
	javaTokens = TestParserHelper.getTokens ();
	settings = new CompilationArguments ();
    }

    @BeforeMethod
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
	cip = new ClassInformationProvider (diagnostics, settings);
	cip.scanClassPath ();
    }

    @Test
    public void testFullNameNoPackageSet () {
	TypeDeclaration t1 = getFirstType ("class C { C c; }");
	checkFieldType (t1, "c", "C");
    }

    @Test
    public void testFullNameInPackageSet () {
	TypeDeclaration t1 = getFirstType ("package foo.bar; class C { C c; }");
	checkFieldType (t1, "c", "foo.bar.C");
    }

    @Test
    public void testInnerClassNameSet () {
	TypeDeclaration t1 = getFirstType ("package foo.bar; class C { class I { I i; } }");
	TypeDeclaration inner = t1.getInnerClasses ().get (0);
	checkFieldType (inner, "i", "foo.bar.C$I");
    }

    @Test
    public void testTypeParamFieldSet () {
	TypeDeclaration t1 = getFirstType ("package foo; class C<T> { T t; }");
	checkFieldType (t1, "t", "java.lang.Object", "T");
    }

    @Test
    public void testFieldSetToOtherClass () {
	TypeDeclaration t1 = getFirstType ("package foo; class C { D d; } class D {}");
	checkFieldType (t1, "d", "foo.D");
    }

    @Test
    public void testSuperClassInnerClassFound () {
	List<TypeDeclaration> types = getTypes ("package foo; class A { class I {} } class B extends A { I i; }");
	TypeDeclaration b = types.get (1);
	checkFieldType (b, "i", "foo.A$I");
    }

    @Test
    public void testFindsGenericTypeOverSuperClassInnerclass () {
	List<TypeDeclaration> types = getTypes ("package foo; class A { class T {} } class B<T> extends A { T t; }");
	TypeDeclaration b = types.get (1);
	checkFieldType (b, "t", "java.lang.Object", "T");
    }

    @Test
    public void testGenericTypeExtends () {
	TypeDeclaration t1 = getFirstType ("package foo; class C<T extends Runnable> { T t; }");
	checkFieldType (t1, "t", "java.lang.Runnable", "T");
    }

    @Test
    public void testInnerClassShadowingSuperClassInner () {
	List<TypeDeclaration> types = getTypes ("package foo; class A { class T {} } class B extends A { class T {} T t; }");
	TypeDeclaration b = types.get (1);
	checkFieldType (b, "t", "foo.B$T");
    }

    @Test
    public void testFieldIsInnerClassWhenTypeHasGenericTypeWithSameName () {
	TypeDeclaration t1 = getFirstType ("package foo; class C<T> { class T {} T t; }");
	checkFieldType (t1, "t", "foo.C$T");
    }

    @Test
    public void testFieldIsGenericTypeEvenThoughSuperClassHasInnnerClass () {
	List<TypeDeclaration> types = getTypes ("package foo; class A { class T {}} class B<T> extends A { T t; }");
	TypeDeclaration b = types.get (1);
	checkFieldType (b, "t", "java.lang.Object", "T");
    }

    @Test
    public void testOuterOuterClassFound () {
	List<TypeDeclaration> types = getTypes ("package foo; class A { class T {} } class B extends A {} class C extends B { T t; }");
	TypeDeclaration c = types.get (2);
	checkFieldType (c, "t", "foo.A$T");
    }

    @Test
    public void testReturnClassGenericType () {
	TypeDeclaration t1 = getFirstType ("package foo; class C<T> { T foo () { return null; }}");
	MethodDeclarationBase md = t1.getMethods ().get (0);
	checkType ((ClassType)md.getResult (), "java.lang.Object", "T");
    }

    @Test
    public void testReturnMethodGenericType () {
	TypeDeclaration t1 = getFirstType ("package foo; class C<T> { <K> K foo () { return null; }}");
	MethodDeclarationBase md = t1.getMethods ().get (0);
	checkType ((ClassType)md.getResult (), "java.lang.Object", "K");
    }

    @Test
    public void testCallingInstanceMethodFromStaticInnerClass () {
	getFirstType ("""
		      class O {
			  static class C extends ClassLoader {
			      byte[] data;
			      protected Class<?> findClass (String name) {
				  return defineClass (name, data, 0, data.length);
			      }
			  }
		      }
		      """);
    }

    @Test
    public void testForExpressionTestOnFieldAccess () {
	getFirstType ("class C { void r (int... e) { for (int i = 0; i < e.length; i++) { }}}");
    }

    @Test
    public void testMethodReturnIsInnnerClassWhenClassHasGenericTypeWithSameName () {
	TypeDeclaration t1 = getFirstType ("package foo; class C<T> { class T {} T foo () { return null; }}");
	MethodDeclarationBase md = t1.getMethods ().get (0);
	checkType ((ClassType)md.getResult (), "foo.C$T", null);
    }

    @Test
    public void testFieldFoundOverGenericType () {
	getFirstType ("package foo; class C<T> { String T; Object foo () { return T; }}");
	assert diagnostics.errorCount () == 0 : "Errors found";
    }

    @Test
    public void testJavaLangImplicitImport () {
	TypeDeclaration t1 = getFirstType ("package foo; class C { String s; }");
	checkFieldType (t1, "s", "java.lang.String");
    }

    @Test
    public void testSingleTypeImportedClass () {
	TypeDeclaration t1 = getFirstType ("package foo; import java.util.Map; class C { Map<C, C> m; }");
	checkFieldType (t1, "m", "java.util.Map");
    }

    @Test
    public void testPackageImport () {
	TypeDeclaration t1 = getFirstType ("package foo; import java.util.*; class C { Map<C, C> m; }");
	checkFieldType (t1, "m", "java.util.Map");
    }

    @Test
    public void testNoErrorsOnInstanceMethodCall () {
	testTypeSetOnVariable ("package foo; class A { void foo (A a) { a.foo (null); }}", "foo.A");
    }

    @Test
    public void testNoErrorsOnFieldMethodCall () {
	testTypeSetOnVariable ("package foo; class A { A a; void foo (Object o) { a.foo (null); }}", "foo.A");
    }

    @Test
    public void testNoErrorsOnStaticMethodCall () {
	testTypeSetOnVariable ("package foo; class A { void foo () { String.join (\".\", \"a\", \"b\"); }}", "java.lang.String");
    }

    @Test
    public void testNoErrorsOnFQNStaticMethodCall () {
	testTypeSetOnVariable ("package foo; class A { void foo () { java.lang.String.join (\".\", \"a\", \"b\"); }}", "java.lang.String");
    }

    private void testTypeSetOnVariable (String txt, String expectedType) {
	TypeDeclaration t = getFirstType (txt);
	MethodDeclarationBase md = t.getMethods ().get (0);
	Block block = (Block)md.getMethodBody ();
	List<ParseTreeNode> statements = block.get ();
	ExpressionStatement es = (ExpressionStatement)statements.get (0);
	MethodInvocation mi = (MethodInvocation)es.getStatement ();
	ParseTreeNode on = mi.getOn ();
	DottedName dn = (DottedName)on;
	FullNameHandler fn = dn.fullName ();
	assert fn != null : "Expected to find type";
	assert fn.getFullDotName ().equals (expectedType) : "Expected to have correct type, but got: " + fn.getFullDotName ();
	assert diagnostics.errorCount () == 0 : "Errors found";
    }

    @Test
    public void testNoErrorsOnStaticFieldMethodCall () {
	getFirstType ("package foo; class A { void foo () { System.out.println (\"Hello World!\"); }}");
	assert diagnostics.errorCount () == 0 : "Errors found";
    }

    @Test
    public void testNoErrorsOnFQNStaticFieldMethodCall () {
	getFirstType ("package foo; class A { void foo () { java.lang.System.out.println (\"Hello World!\"); }}");
	assert diagnostics.errorCount () == 0 : "Errors found";
    }

    @Test
    public void testFieldConflictsWithInnerClass () {
	getFirstType ("class G { class T{}; String T; void foo () { T = null; }}");
	assert diagnostics.errorCount () == 0 : "Errors found";
    }

    @Test
    public void testOwnPrivateFieldIsAccessible () {
	getFirstType ("class C { private String s; void foo () { s.length (); }}");
	assert diagnostics.errorCount () == 0 : "Errors found";
    }

    @Test
    public void testOtherPrivateFieldIsAccessible () {
	// we only want one error for this.
	getTypes ("class C { private String s; } class D { void foo (C c) { c.s.length (); }}", 1);
    }

    @Test
    public void testSeveralNestedFields () {
	getTypes ("class C { C c; C foo () { return c.c.c.c.c; }}");
    }

    @Test
    public void testSeveralNestedClasses () {
	getTypes ("package foo; class C { class D { class E { class F {}}} void foo () {foo.C.D.E.F f = null; }}");
    }

    @Test
    public void testAmbigousNameWithPrimitiveTypeFromClasspathClass () {
	getFirstType ("""
		      import java.io.File;
		      class Crash {
			  void getPath () {
			      String pkg = "foo.bar";
			      String p = pkg.replace ('.', File.separatorChar);
			  }
		      }
		      """);
    }

    @Test
    public void testAmbigousNameWithPrimitiveTypeFromCompiledClass () {
	getFirstType ("""
		      class F { public static int P = 1; }
		      class C { void foo () { int f = F.P; }}
		      """);
    }

    @Test
    public void testMissingMethodGivesError () {
	getTypes ("class C { void foo (C c) { c.bar(); }}", 1);
    }

    @Test
    public void testMissingArgumentGivesError () {
	getTypes ("class C { void foo (C c) { c.foo(); }}", 1);
    }

    @Test
    public void testTooManyArgumentsGivesError () {
	getTypes ("class C { void foo (C c) { c.foo(null, null); }}", 1);
    }

    @Test
    public void testCallingInstanceMethodOnClassGivesError () {
	getTypes ("class C { void foo () { String.equals (null); }}", 1);
    }

    @Test
    public void testStaticMethodAccessingInstanceFieldGivesError () {
	getTypes ("class C { int x; static void foo () { x++; }}", 1);
    }

    @Test
    public void testInnerClassCanAccessFieldsInsideStaticMethod () {
	getTypes ("class C { static class I { int x; void foo () { x = 3; }}}", 0);
    }

    @Test
    public void testStaticMethodAccessingStaticFieldGivesNoErrors () {
	getTypes ("class C { static int x; static void foo () { x++; }}", 0);
    }

    @Test
    public void testMethodAccessingFieldGivesNoErrors () {
	getTypes ("class C { int x; void foo () { x++; }}", 0);
    }

    @Test
    public void testLocalVariableIsNotAccessibleAfterBlock () {
	getTypes ("class C { void foo () { int x = 3; { int y; y = x + 3;} y++; }}", 1);
    }

    @Test
    public void testLocalVariableWrongTypeInInitializer () {
	getTypes ("class C { void foo () { int x = 3L; }}", 1);
    }

    @Test
    public void testFieldWrongTypeInInitializer () {
	getTypes ("class C { int x = 3L; }", 1);
    }

    @Test
    public void testStaticMethodAccessingInstanceFieldsWithOuterClassHavingStaticFieldGivesError () {
	getTypes ("class O { static int x; class C { int x; static void foo () { x++; }}}", 1);
    }

    @Test
    public void accessingFieldsFromTypeGivesError () {
	getTypes ("import java.awt.Point; class C { void foo () { int x = Point.x; }}", 1);
    }

    @Test
    public void accessingFieldsOnInstanceGivesNoError () {
	getTypes ("import java.awt.Point; class C { void foo (Point p) { int x = p.x; }}", 0);
    }

    @Test
    public void testAccessingFieldFromMethodCall () {
	getTypes ("class C { int X = 2; C foo () { return new C (); } void bar () { int y = foo().X; }}");
    }

    @Test
    public void testrReturnArrayLength () {
	getTypes ("class C { int foo (int[] arr) { return arr.length; }}");
    }

    @Test
    public void testrAccessingNonExistingArrayField () {
	getTypes ("class C { int foo (int[] arr) { return arr.ewqrwe; }}", 1);
    }

    @Test
    public void testArrayLengthAccess () {
	getTypes ("class C { int foo (int[] arr) { return arr.length.qwer; }}", 1);
    }

    @Test
    public void testVarArgParameterSetting () {
	getTypes ("class C { void foo (Object... arr) { arr[3].toString (); }}");
    }

    @Test
    public void testVarArgAsArgument () {
	getTypes ("class T {} class C { static void a (T... tokens) { String s = java.util.Arrays.toString (tokens); }}");
    }

    @Test
    public void testVarArgWithTooManyArgs () {
	getTypes ("class T {} class C { void foo (String... args) {} " +
		  "void bar (String s1, String s2) { foo (new String[]{\"a\"}, s1, s2); }}", 1);
    }

    @Test
    public void testVarArgWithNonMatchingPart () {
	getTypes ("class T {} class C { void foo (String... args) {} " +
		  "void bar (String s1, String s2) { foo (s1, s2, new String[]{\"a\"}); }}", 1);
    }

    @Test
    public void testVarArgWithNoArgs () {
	getTypes ("class T {} class C { void foo (Object... args) {} " +
		  "void bar () { foo (); }}", 0, 0);
    }

    @Test
    public void testVarArgWithArray () {
	getTypes ("class T {} class C { void foo (Object... args) {} " +
		  "void bar (String[] s1) { foo (s1); }}", 0, 1);
    }

    @Test
    public void testVarArgWithArrayAndMore () {
	getTypes ("class T {} class C { void foo (Object... args) {} " +
		  "void bar (String[] s1, String s2) { foo (s1, s2); }}", 0);
    }

    @Test
    public void testVarArgWithArgsAndArray () {
	getTypes ("class T {} class C { void foo (Object... args) {} " +
		  "void bar (String s1, String[] s2) { foo (s1, s2); }}", 0);
    }

    @Test
    public void testClassLiteral () {
	getTypes ("class C { void foo () { C.class.getResource (\"a\"); }}");
    }

    @Test
    public void testConstructorParameterIsUsable () {
	getTypes ("class C { C (String s) { int l = s.length (); }}");
    }

    @Test
    public void testFieldsFromSuperClassIsFound () {
	getTypes ("class B { int flags; } class C extends B { C () { flags = 3; }}");
    }

    @Test
    public void testThisFieldsFromSuperClassIsFound () {
	getTypes ("class B { int flags; } class C extends B { C () { this.flags = 3; }}");
    }

    @Test
    public void testMethodIsFound () {
	getTypes ("class C { void foo () {} void bar () { foo (); }}");
    }

    @Test
    public void testThisMethodIsFound () {
	getTypes ("class C { void foo () {} void bar () { this.foo (); }}");
    }

    @Test
    public void testStaticMethodIsFound () {
	getTypes ("class C { static void foo () {} void bar () { this.foo (); }}");
    }

    @Test
    public void testInstanceMethodNotAllowedFromStatic () {
	getTypes ("class C { void foo () {} static void bar () { foo (); }}", 1);
    }

    @Test
    public void testInstanceMethodNotAllowedFromStaticThis () {
	getTypes ("class C { void foo () {} static void bar () { this.foo (); }}", 1);
    }

    @Test
    public void testSuperClassMethodIsFound () {
	getTypes ("class B { void foo () {} } class C extends B { void bar () { foo (); }}");
    }

    @Test
    public void testObjectCloneIsNotUsable () {
	getTypes ("class C { void bar () { Object a = new Object (); Object b = a.clone (); }}", 1);
    }

    @Test
    public void testArrayCloneIsUsable () {
	getTypes ("class C { void bar () { Object[] a = {}; Object[] b = a.clone (); }}", 0);
    }

    @Test
    public void testThisPrimaryUsedForMethod () {
	getTypes ("class C { void foo () { String s = this.getClass ().getName (); }}");
    }

    @Test
    public void testMethodFromOuterClassIsReachable () {
	getTypes ("class C { void foo () {} class D { void bar () { foo (); }}}");
    }

    @Test
    public void testMethodArgsFound () {
	getTypes ("class C { void foo (String[] args) {if (args[3].equals (\"-\")) {}}}");
    }

    @Test
    public void testStaticMethodArgsFound () {
	getTypes ("class C { static void foo (String[] args) {if (args[3].equals (\"-\")) {}}}");
    }

    @Test
    public void testTernary () {
	getTypes ("class C { void foo (boolean b) { int x = (b ? \"Yes\" : \"No\").length (); }}");
    }

    @Test
    public void testCast () {
	getTypes ("class C { void foo () { Object o = \"a\"; int l = ((String)o).length (); }}");
    }

    @Test
    public void testImpossibleCast () {
	getTypes ("class C { void foo () { Object c = new C (); int l = ((String)c).length (); }}", 0);
	getTypes ("class C { void foo () { C c = new C (); int l = ((String)c).length (); }}", 1);
    }

    @Test
    public void testNonNeededPrimitiveCastGiveWarning () {
	getTypes ("class C { void foo () { double d = 3.4; double f = (double)d; }}", 0, 1);
    }

    @Test
    public void testNonNeededCastGiveWarning () {
	getTypes ("class C { void foo () { C c = new C(); C d = (C)c; }}", 0, 1);
    }

    @Test
    public void testNonNeededCastGiveWarningForSubclassToSuperclassCast () {
	getTypes ("class A {} class C extends A { void foo () { C c = new C(); A a = (A)c; }}", 0, 1);
    }

    @Test
    public void testPrimitiveCasts () {
	getTypes ("class C { byte b = (byte)34; }");
	getTypes ("class C { short b = (short)23; }");
	getTypes ("class C { int b = (int)23L; }");
	getTypes ("class C { float b = (float)4.3; }");
	getTypes ("class C { int x = (int)4.3; }");
    }

    @Test public void testBooleanCasts () {
	getTypes ("class C { boolean x = (boolean)4.3; }", 1);
    }

    @Test public void testWideningCast () {
	// no error and no warning in modern java.
	getTypes ("class C { long x = (long)5; }");
    }

    @Test public void testTooLargeByte () {
	getTypes ("class C { byte b = 128; }", 1);
    }

    @Test public void testTooSmallByte () {
	getTypes ("class C { byte b = -129; }", 1);
    }

    @Test public void testNegativeWrappedByte () {
	getTypes ("class C { byte x = -(-(-0)); }");
    }

    @Test public void testNegativeByte () {
	getTypes ("class C { byte b = -1; }");
    }

    @Test public void testNegativeShort () {
	getTypes ("class C { short b = -1; }");
    }

    @Test
    public void testTwoPartString () {
	getTypes ("class C { void foo () { int l = (\"a\" + \"b\").length (); }}");
    }

    @Test
    public void testTwoPartStringInt () {
	getTypes ("class C { void foo () { int l = (\"a\" + 1).length (); }}");
    }

    @Test
    public void testTwoPartIntString () {
	getTypes ("class C { void foo () { int l = (1 + \"a\").length (); }}");
    }

    @Test
    public void testTwoPartIntInt () {
	getTypes ("class C { void foo () { int x = 3; int l = 1 + x;}}");
    }

    @Test
    public void testErrorOnMissMatchedReturn () {
	getTypes ("class C { int foo () { double d = 3.0; return d; }}", 1);
    }

    @Test
    public void testNoErrorOnAllowedUpcast () {
	getTypes ("class C { double foo () { int i = 4; return i; }}");
    }

    @Test
    public void testErrorOnMissmatchedReferenceType () {
	getTypes ("class C { String foo () { Object o = new Object (); return o; }}", 1);
    }

    @Test
    public void testNoErrorOnReturningSubtype () {
	getTypes ("class C { Object foo () { String s = \"foo\"; return s; }}");
    }

    @Test
    public void testErrorOnReturningFromVoid () {
	getTypes ("class C { void foo () { String s = \"foo\"; return s; }}", 1);
    }

    @Test
    public void testErrorOnReturningVoidInNonVoidMethod () {
	getTypes ("class C { Object foo () { String s = \"foo\"; return; }}", 1);
    }

    @Test
    public void testTwoPartMissingPartial () {
	getTypes ("class C { void foo () { int l = 1 + x;}}", 1);
    }

    @Test
    public void testIfExpressionNonBoolean () {
	getTypes ("class C { static void r () { if (3) return; }}", 1);
    }

    @Test
    public void testIfExpressionComplex () {
	getTypes ("class C { static int r (String s) { if (s == null) return 1; return 2; }}");
    }

    @Test
    public void testBasicForExpressionMissing () {
	getTypes ("class C { static void r () { for (;;) {}}}");
    }

    @Test
    public void testBasicForExpressionBoolean () {
	getTypes ("class C { static void r () { for (; true; ) {}}}");
    }

    @Test
    public void testBasicForExpressionNonBoolean () {
	getTypes ("class C { static void r () { for (; 34; ) {}}}", 1);
    }

    @Test
    public void testInterfaceInGenericType () {
	getTypes ("interface I {} class B<T extends I> {} class C { B<I> bs; }");
    }

    @Test
    public void testSubtypeGenericType () {
	getTypes ("interface I {} class A implements I {} class B<T extends I> {} class C { B<A> bs; }");
    }

    @Test
    public void testAutoBoxIntMethodArgumentMatchin () {
	getTypes ("class C { static void r () { String.format (\"A: %d\", 12); }}");
    }

    @Test
    public void testAutoBoxToSyperType () {
	getTypes ("class C { void a (Number n) {} void f () { a (1); }}");
    }

    @Test
    public void testAutoBoxToInterface () {
	getTypes ("class C { void a (java.io.Serializable s) {} void f () { a (1); }}");
    }

    @Test
    public void testAutoBoxInReturn () {
	getTypes ("class C { static Integer r () { return 12; }}");
    }

    @Test
    public void testAutoBoxInReturnSuperType () {
	getTypes ("class C { static Number r () { return 12; }}");
    }

    @Test
    public void testAutoBoxInReturnInterface () {
	getTypes ("class C { static java.io.Serializable r () { return 12; }}");
    }

    @Test
    public void testAutoBoxWrongTypeGivesError () {
	// "12" is an int javac gives "incompatible types: int cannot be converted to Long"
	getTypes ("class C { static Long r () { return 12; }}", 1);
    }

    @Test
    public void testAutoUnBoxReturn () {
	getTypes ("class C { static long r () { Long l = 77L; return l; }}");
    }

    @Test
    public void testAutoUnBoxArgument () {
	getTypes ("class C { static void a (long l) {} static void r () { Long l = 77L; a(l); }}");
    }

    @Test
    public void testForEachOnArray () {
	getTypes ("class C { void a () {int[] array = new int[7]; for (int a : array) { }}}");
    }

    @Test
    public void testArrayInitializer () {
	getTypes ("class C { int[] f = {}; }");
	getTypes ("class C { int[] f = {1}; }");
	getTypes ("class C { void a (int i) { int[] arr = {i}; }; }");
	getTypes ("class C { String[] s = {\"one\", \"two\"};}");
	getTypes ("class C { int[] f = {1.5}; }", 1);
    }

    @Test
    public void testForEachOnCollection () {
	// code would give NPE, but we do not care for this test case
	getTypes ("class C { void a () {java.util.List<String> ls = null; for (int a : ls) { }}}");
    }

    @Test
    public void testForEachOnNonIterable () {
	getTypes ("class C { void a () {int x = 3; for (int a : x) { }}}", 1);
    }

    @Test
    public void testGetArrayElementWrongType () {
	getTypes ("class C { void a () {int[] array = new int[7]; String s = array[2]; }}", 1);
    }

    @Test
    public void testLambdaLocalVariable () {
	getTypes ("class C { void a () {Runnable r = () -> System.out.println (); }}");
	getTypes ("class C { void a () {java.util.function.IntConsumer ic = i -> System.out.println (i); }}");
	getTypes ("class C { void a () {java.util.function.IntToDoubleFunction i2d = i -> 3.2 * i; }}");
	getTypes ("class C { void a () {java.util.function.IntToDoubleFunction i2d = i -> \"wrong!\"; }}", 1);
    }

    @Test
    public void testLambdaAssignment () {
	getTypes ("class C { void a () {Runnable r; r = () -> System.out.println (); }}");
	getTypes ("class C { void a () {java.util.function.IntConsumer ic; ic = i -> System.out.println (i); }}");
	getTypes ("class C { void a () {java.util.function.IntToDoubleFunction i2d; i2d = i -> 3.2 * i; }}");
	getTypes ("class C { void a () {java.util.function.IntToDoubleFunction i2d; i2d = i -> \"wrong!\"; }}", 1);
    }

    @Test
    public void testLambdaMethodArgument1 () {
	getTypes ("class C { void r (Runnable r) { r.run (); } void a () { r (() -> System.out.println ()); }}");
	getTypes ("class C { void r (java.util.function.IntConsumer r) { } void a () { r (i -> System.out.println ()); }}");
	getTypes ("class C { void r (java.util.function.IntConsumer r) { } void a () { r ((int i) -> System.out.println ()); }}");
	getTypes ("class C { void r (java.util.function.IntToDoubleFunction r) { } void a () { r ((int i) -> \"wrong\"); }}", 1);
    }

    @Test
    public void testLambdaMethodArgument2 () {
	getTypes ("class C { void a () { java.util.List<String> ls; x (t -> ls.add (t)); } <T> void x (java.util.function.Consumer<T> c) { }}");
    }

    @Test
    public void testVoidLambdaEvaluatesToInt () {
	getTypes ("class C { int x; void a () {Runnable r = () -> x = 37; }}");
    }

    @Test
    public void testIntLambdaEvaluatesToVoid () {
	getTypes ("class C { int x; void a () {java.util.function.IntSupplier s = () -> System.out.println (); }}", 1);
    }

    /*
    @Test
    public void testLambdaExpressionContent () {
	getTypes ("class C { void foo () { java.util.List<String> ls = null; ls.stream ().filter (i -> !i.isEmpty ()).forEach(i -> { }); }}", 0);
    }
    */

    @Test
    public void testInstanceofInReturn () {
	getTypes ("class C { boolean a (Object o) { return o instanceof String s; }}");
    }

    @Test
    public void testInstanceofVariableAccess () {
	getTypes ("class C { boolean a (Object o) { int len = 0; if (o instanceof String s) len = s.length (); }}");
    }

    @Test
    public void testSimpleMethodReference () {
	getTypes ("class C { void a () { Runnable r = this::b; r.run (); } void b () {}}");
	getTypes ("class C { void a () { Runnable r; r = this::b; r.run (); } void b () {}}");
	getTypes ("class C { void a () { java.util.function.IntConsumer ic = this::b; } void b (int i) {}}");
	getTypes ("class C { void a () { java.util.function.IntConsumer ic; ic = this::b; } void b (int i) {}}");
	getTypes ("class C { void a () { java.util.function.IntConsumer ic = this::b; } void b (int i, int j) {}}", 1);
    }

    @Test
    public void testConstructorRefernce () {
	getTypes ("class C { C() {} void a () { Runnable r = C::new; r.run (); }}");
	getTypes ("class C { C() {} static void a () { Runnable r = C::new; r.run (); }}");
	getTypes ("class C { C(int i) {} void a () { Runnable r = C::new; r.run (); }}", 1);
    }

    @Test
    public void testSuperMethodReference () {
	getTypes ("class A { void foo () {}} class C extends A { void foo () {} void c () { Runnable r = super::foo; }}");
	getTypes ("class A { void foo () {}} class C extends A { void foo () {} void c () { Runnable r = C.super::foo; }}");
	getTypes ("class A { public String toString () { return \"A\"; } class C { void c () { Runnable r = A.super::toString; }}}");

	// A is not an enclosing type of C
	getTypes ("class A { void foo () {}} class C extends A { void foo () {} void c () { Runnable r = A.super::foo; }}", 1);
    }

    @Test
    public void testCallingStaticMethodReferenceUsingThis () {
	getTypes ("class C { void a () { Runnable r = C::b; r.run (); } static void b () {}}");

	// javac say: "unexpected static method b() found in bound lookup"
	getTypes ("class C { void a () { Runnable r = this::b; r.run (); } static void b () {}}", 1);
    }

    @Test
    public void testCallingInstanceMethodReferenceUsingInStatic () {
	// javac say: "unexpected instance method b() found in unbound lookup"
	getTypes ("class C { static void a () { Runnable r = C::b; r.run (); } void b () {}}", 1);
    }

    @Test
    public void testCallingInstanceMethodReferenceOnObjectInsideStatic () {
	getTypes ("class C { static void a (C c) { Runnable r = c::b; r.run (); } void b () {}}");
    }

    @Test
    public void testSwitchExpression () {
	getTypes ("class C { static void a (String s) {Object o = switch (s) { case \"foo\" -> 1; case \"bar\" -> \"what\"; default -> null; }; }}");
    }

    @Test
    public void testFieldFromSuperSuper () {
	getTypes ("class A { int f; } class B extends A {} class C extends B { public C () {f = 7;}}");
    }

    @Test
    public void testMethodChainingMissmatch () {
	// we used to crash when we found no methods at all.
	getTypes ("class C { static int x () { return 6; } public static void a () { int l = x().length (); }}", 1);
    }

    @Test
    public void testThrowsAreThrowable () {
	// we used to crash when we found no methods at all.
	getTypes ("class C { static void a () throws Exception { }}", 0);
	getTypes ("class C { static void a () throws java.io.IOException { }}", 0);
	getTypes ("class C { static void a () throws Integer { }}", 1);
    }

    @Test
    public void testUnaryNot () {
	getTypes ("class C { void a () { boolean b = true; b = !b; }}", 0);
	getTypes ("class C { void a () { int b = 0; b = !b; }}", 1);
    }

    @Test
    public void testUnaryTildeIntegral () {
	getTypes ("class C { void a () { int b = 0; b = ~b; }}", 0);
    }

    @Test
    public void testUnaryTildeDouble () {
	getTypes ("class C { void a () { double b = 0; b = ~b; }}", 1);
    }

    @Test
    public void testUnaryTildeBoolean () {
	getTypes ("class C { void a () { boolean b = true; b = ~b; }}", 1);
    }

    @Test
    public void testUnaryTildeAutoBox () {
	getTypes ("class C { int a (Integer x) { return ~x; }}", 0);
    }

    @Test
    public void testNullComparisson () {
	getTypes ("class C { boolean b = null == new Object (); }");
	getTypes ("class C { boolean b = null != new Object (); }");
	getTypes ("class C { int b = 3; int x = 0; { if (b == null) x = 1; }}", 1);
    }

    @Test
    public void testDimExprWithDoubleGivesError () {
	getTypes ("class C { int x = 3; int[] a = new int[x]; }", 0);
	getTypes ("class C { int[] a = new int[3.14]; }", 1);
    }

    @Test
    public void testNotInIf () {
	getTypes ("import java.util.Set; class C { int foo () { Set<String> s = null; String t = \"a\"; if (!s.contains (t)) return 3; return 4; }}");
    }

    @Test
    public void testYieldVoid () {
	getTypes ("class C { int foo (int i) { return switch (i) { case 0 -> { yield bar (); } default -> 14; };} void bar () { }}", 1);
    }

    @Test
    public void testAssertTestType () {
	getTypes ("class C { void foo (boolean b) { assert b; }}", 0);
	getTypes ("class C { void foo (int i) { assert i; }}", 1);
    }

    @Test
    public void testAssertErrorMessageType () {
	getTypes ("class C { void foo (boolean b) { assert b : \"asdf\"; }}", 0);
	getTypes ("class C { void foo (boolean b) { assert b : bar (); } void bar () {}}", 1);
    }

    /* TODO: implement full generic handling */
    /*
    @Test
    public void testMissMatchedGenericTypes () {
	getTypes ("import java.util.Map; class C { Map<String> m; void foo () { int l = m.size (); }}", 1);
    }

    @Test
    public void testWrongGenericType () {
	getTypes ("interface I {} class B<T extends I> {} class C { B<String> bs; }", 1);
    }

    @Test
    public void testGenericReturnFromResourceClass () {
	getTypes ("import java.util.Map; class C { Map<String, String> m; void foo () { int l = m.get (\"\").length (); }}");
    }

    @Test
    public void testGenericReturnFromCompiledClass () {
	getTypes ("class C<T> { T t; T a() { return t; } void b () { T t1 = a (); }}");
    }

    @Test
    public void testFieldFromGenericCompiledClass () {
	getTypes ("class A { String s; } class C { java.util.Map<String, A> m; void c () { String s = m.get (\"\").s; }}");
    }
    */

    private TypeDeclaration getFirstType (String txt) {
	return getTypes (txt).get (0);
    }

    private List<TypeDeclaration> getTypes (String txt) {
	return getTypes (txt, 0);
    }

    private List<TypeDeclaration> getTypes (String txt, int expectedErrors) {
	return getTypes (txt, expectedErrors, 0);
    }

    private List<TypeDeclaration> getTypes (String txt, int expectedErrors, int expectedWarnings) {
	ParsedEntry tree = syntaxTree (txt);
	cip.addTypes (tree.getRoot (), tree.getOrigin ());
	ClassSetter.fillInClasses (javaTokens, cip, List.of (tree), diagnostics);
	assert diagnostics.errorCount () == expectedErrors
	    : String.format ("Got unexpected number of errors: %d, expected %d: errors:\n%s",
			     diagnostics.errorCount (), expectedErrors, TestParserHelper.getParseOutput (diagnostics));
	assert diagnostics.warningCount () == expectedWarnings
	    : String.format ("Got unexpected number of warnings: %d, expected %d: warnings:\n%s",
			     diagnostics.warningCount (), expectedWarnings, TestParserHelper.getParseOutput (diagnostics));

	OrdinaryCompilationUnit ocu = (OrdinaryCompilationUnit)tree.getRoot ();
	return ocu.getTypes ();
    }

    private ParsedEntry syntaxTree (String txt) {
	DirAndPath dirPath = new DirAndPath (Paths.get ("."), Paths.get ("C.java"));
	ParseTreeNode syntaxTree = TestParserHelper.syntaxTree (grammar, txt, diagnostics);
	return new ParsedEntry (dirPath, syntaxTree);
    }

    private void checkFieldType (TypeDeclaration t1, String fieldName, String expectedFieldType) {
	checkFieldType (t1, fieldName, expectedFieldType, null);
    }

    private void checkFieldType (TypeDeclaration t1, String fieldName,
				 String expectedFieldType, String expectedTypeParam) {
	Map<String, FieldInfo> fields = t1.getFields ();
	FieldInfo fi = fields.get (fieldName);
	checkType ((ClassType)fi.type (), expectedFieldType, expectedTypeParam);
    }

    private void checkType (ClassType type, String expectedType, String expectedTypeParam) {
	FullNameHandler fn = type.fullName ();
	assert fn != null : "Expected full name to be set";
	String dollarName = fn.getFullDollarName ();
	TypeParameter tp = type.getTypeParameter ();
	assert expectedType.equals (dollarName) : "Expected type to be " + expectedType + ", but got: " + dollarName +
	    (tp != null ? " with type parameter: " + tp : "");
	if (expectedTypeParam == null)
	    assert tp == null : "Expected to not have any type paramter";
	else
	    assert expectedTypeParam.equals (tp.getId ()) : "Got unexpected name: " + tp.getId ();
    }
}
