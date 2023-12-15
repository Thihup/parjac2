package org.khelekore.parjac2.javacompiler.parse;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.TestParserHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestLocalClassnames {
    private Grammar g;
    private CompilerDiagnosticCollector diagnostics;

    @BeforeClass
    public void createLRParser () {
	g = TestParserHelper.getJavaGrammarFromFile ("CompilationUnit", false);
    }

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testSimpleLocalClass () {
	testSuccessfulParse ("""
			     class LC {
                                 public void foo () {
				     class Local {};
				     class Other {}
                                 }
                             }""",
                             2);

    }

    @Test
    public void testDuplicateLocalClasses () {
	testSuccessfulParse ("""
			     class LC {
                                 public void foo () {
				     class Local {};
                                 }
				 public void bar () {
                                     class Local {};
				 }
                             }""",
			     2);

    }

    @Test
    public void testBlockLevelDuplicates () {
	testSuccessfulParse ("""
			     class LC {
                                 public void foo () {
				     {
					 class Local {};
				     }
				     {
					 class Local {};
				     }
				 }
                             }""",
			     2);

    }

    private void testSuccessfulParse (String s, int numLocalClasses) {
 	ParseTreeNode p = TestParserHelper.testSuccessfulParse (g, s, diagnostics, null);
	OrdinaryCompilationUnit o = (OrdinaryCompilationUnit)p;
	TypeDeclaration t = o.getTypes ().get (0); // should only be one class.
	List<TypeDeclaration> innerClasses = t.getInnerClasses ();
	assert innerClasses.size () == numLocalClasses : "Expected " + numLocalClasses + ", but got: " + innerClasses.size ();
	Set<String> names = new HashSet<> ();
	innerClasses.forEach (i -> names.add (i.getLocalName ()));
	assert names.size () == numLocalClasses : "Expected " + numLocalClasses + " unique names, got :" + names;
    }
}
