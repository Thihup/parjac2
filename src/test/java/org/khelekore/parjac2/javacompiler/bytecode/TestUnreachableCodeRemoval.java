package org.khelekore.parjac2.javacompiler.bytecode;

import java.util.List;

import org.khelekore.parjac2.javacompiler.compile.CompileAndRun;

import org.testng.annotations.Test;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.CodeModel;
import io.github.dmlloyd.classfile.Instruction;
import io.github.dmlloyd.classfile.MethodModel;

public class TestUnreachableCodeRemoval extends CompileAndRun {

    @Test
    public void testCodeFollowingTrueIfWithReturnIsRemoved () {
	CodeModel code = getMethod ("C.java", "class C { int foo () { if (true) return 3; return 5; }}", "C", "foo");
	long numInstructions = code.elementStream ().filter (ce -> ce instanceof Instruction).count ();
	// should only be ldc 3 and ireturn.
	assert numInstructions == 2 : "Unexpected number of instructions: got: " + numInstructions + ", expected: " + 2;
    }

    public CodeModel getMethod (String filename, String text, String classname, String methodName) {
	byte[] bytecode = compileAndGetBytecode (filename, text).get (classname);
	ClassModel model = ClassFile.of ().parse (bytecode);
	List<MethodModel> methods = model.methods ();
	for (MethodModel mm : methods) {
	    if (mm.methodName ().stringValue ().equals (methodName))
		return mm.code ().get ();
	}
	throw new IllegalStateException ("Method " + methodName + " not found");
    }
}
