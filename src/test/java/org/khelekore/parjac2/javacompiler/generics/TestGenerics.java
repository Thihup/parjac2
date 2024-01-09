package org.khelekore.parjac2.javacompiler.generics;

import org.khelekore.parjac2.javacompiler.semantic.TestCompilationErrorHandling;

import org.testng.annotations.Test;

public class TestGenerics extends TestCompilationErrorHandling {

    /* TODO implement code to make test work
    @Test
    public void genericClassUsedWithoutGenericsGivesWarning () {
	testClass ("C.java", "import java.util.List; class C { List l; }", 0, 1);
    }
    */

    @Test
    public void genericClassUsedWithGenericsGivesNoWarning () {
	testClass ("C.java", "import java.util.List; class C { List<String> l; }", 0, 0);
	testClass ("C.java", "import java.util.Map; class C { Map<String, String> m; }", 0, 0);
    }

    /* TODO implement code to make test work
    @Test
    public void genericClassUsedWithTooManyGenericsGivesError () {
	testClass ("C.java", "import java.util.List; class C { List<String, String> l; }", 1, 0);
    }
    */
}
