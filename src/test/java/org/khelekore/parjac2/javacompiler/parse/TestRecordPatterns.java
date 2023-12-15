package org.khelekore.parjac2.javacompiler.parse;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class TestRecordPatterns extends CompilationUnitTest {

    @BeforeTest
    public void createDiagnostics () {
	diagnostics = new CompilerDiagnosticCollector ();
    }

    @Test
    public void testRecordPatterns16 () {
	String code = """
	    class Foo {
		record Point(int x, int y) {}
		static void printSum(Object obj) {
		    if (obj instanceof Point p) {
		    }
		}
	    }
	""";
	    testSuccessfulParse (code);
    }

    @Test
    public void testRecordPatterns21 () {
	String code = """
	    class Foo {
		record Point(int x, int y) {}
		static void printSum(Object obj) {
		    if (obj instanceof Point(int x, int y)) {
		    }
		}
	    }
	""";
	    testSuccessfulParse (code);
    }
}
