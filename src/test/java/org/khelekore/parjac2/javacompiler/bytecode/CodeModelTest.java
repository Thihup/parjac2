package org.khelekore.parjac2.javacompiler.bytecode;

import java.util.List;

import org.khelekore.parjac2.javacompiler.compile.CompileAndRun;

import io.github.dmlloyd.classfile.ClassFile;
import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.CodeModel;
import io.github.dmlloyd.classfile.MethodModel;

public class CodeModelTest extends CompileAndRun {

    public CodeModel getCodeModel (String filename, String text, String classname, String methodName) {
	return getMethodModel (filename, text, classname, methodName).code ().get ();
    }

    public MethodModel getMethodModel (String filename, String text, String classname, String methodName) {
	byte[] bytecode = compileAndGetBytecode (filename, text).get (classname);
	ClassModel model = ClassFile.of ().parse (bytecode);
	List<MethodModel> methods = model.methods ();
	for (MethodModel mm : methods) {
	    if (mm.methodName ().stringValue ().equals (methodName)) {
		return mm;
	    }
	}
	throw new IllegalStateException ("Method " + methodName + " not found");
    }
}
