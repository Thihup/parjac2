package org.khelekore.parjac2.javacompiler.bytecode;

import org.testng.annotations.Test;

import io.github.dmlloyd.classfile.CodeModel;
import io.github.dmlloyd.classfile.Instruction;

public class TestUnreachableCodeRemoval extends CodeModelTest {

    @Test
    public void testCodeFollowingTrueIfWithReturnIsRemoved () {
	CodeModel code = getCodeModel ("C.java", "class C { int foo () { if (true) return 3; return 5; }}", "C", "foo");
	long numInstructions = code.elementStream ().filter (ce -> ce instanceof Instruction).count ();
	// should only be ldc 3 and ireturn.
	assert numInstructions == 2 : "Unexpected number of instructions: got: " + numInstructions + ", expected: " + 2;
    }
}
