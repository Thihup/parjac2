package org.khelekore.parjac2.javacompiler;

import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclarationBase;
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

    /* TODO: ClassSetter needs a fix */
    /*
    @Test
    public void testMethodReturnIsInnnerClassWhenClassHasGenericTypeWithSameName () {
	TypeDeclaration t1 = getFirstType ("package foo; class C<T> { class T {} T foo () { return null; }}");
	MethodDeclarationBase md = t1.getMethods ().get (0);
	checkType ((ClassType)md.getResult (), "foo.C$T", null);
    }
    */

    @Test
    public void testFieldSetToOtherClass () {
	TypeDeclaration t1 = getFirstType ("package foo; class C { D d; } class D {}");
	checkFieldType (t1, "d", "foo.D");
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

    private TypeDeclaration getFirstType (String txt) {
	ParsedEntry tree = syntaxTree (txt);
	cip.addTypes (tree.getRoot (), tree.getOrigin ());
	ClassSetter.fillInClasses (cip, List.of (tree), diagnostics);
	assert !diagnostics.hasError () : "Got parser errors: " + TestParserHelper.getParseOutput (diagnostics);
	OrdinaryCompilationUnit ocu = (OrdinaryCompilationUnit)tree.getRoot ();
	List<TypeDeclaration> ls = ocu.getTypes ();
	return ls.get (0);
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
	FieldDeclarationBase fd = fi.fd ();
	checkType ((ClassType)fd.getType (), expectedFieldType, expectedTypeParam);
    }

    private void checkType (ClassType type, String expectedType, String expectedTypeParam) {
	FullNameHandler fn = type.getFullNameHandler ();
	assert fn != null : "Expected full name to be set";
	String dollarName = fn.getFullDollarName ();
	TypeParameter tp = type.getTypeParameter ();
	assert expectedType.equals (dollarName) : "Expected name to be set correctly, but got: " + dollarName +
	    (tp != null ? " with type parameter: " + tp : "");
	if (expectedTypeParam == null)
	    assert tp == null : "Expected to not have any type paramter";
	else
	    assert expectedTypeParam.equals (tp.getId ()) : "Got unexpected name: " + tp.getId ();
    }
}
