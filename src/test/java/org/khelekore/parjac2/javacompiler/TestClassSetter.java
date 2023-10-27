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
    private CompilationArguments settings;

    private CompilerDiagnosticCollector diagnostics;
    private ClassInformationProvider cip;

    @BeforeClass
    public void createTools () {
	grammar = TestParserHelper.getJavaGrammarFromFile ("CompilationUnit", false);
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
	testTypeSetOnVariable ("package foo; class A { A a; void foo () { a.foo (null); }}", "foo.A");
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
	FullNameHandler fn = dn.getFullNameHandler ();
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
	getTypes ("class C { private String s; } class D { void foo (C c) { c.s.length (); }}", 1);
    }

    @Test
    public void testSeveralNestedFields () {
	getTypes ("class C { C c; void foo () { return c.c.c.c.c; }}");
    }

    @Test
    public void testSeveralNestedClasses () {
	getTypes ("package foo; class C { class D { class E { class F {}}} void foo () {foo.C.D.E.F f = null; }}");
    }

    private TypeDeclaration getFirstType (String txt) {
	return getTypes (txt).get (0);
    }

    private List<TypeDeclaration> getTypes (String txt) {
	return getTypes (txt, 0);
    }

    private List<TypeDeclaration> getTypes (String txt, int expectedErrors) {
	ParsedEntry tree = syntaxTree (txt);
	cip.addTypes (tree.getRoot (), tree.getOrigin ());
	ClassSetter.fillInClasses (cip, List.of (tree), diagnostics);
	assert diagnostics.errorCount () == expectedErrors : "Got unexpected number of errors: " + TestParserHelper.getParseOutput (diagnostics);
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
	FullNameHandler fn = type.getFullNameHandler ();
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
