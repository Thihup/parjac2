package org.khelekore.parjac2.javacompiler.bytecode;

import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import org.testng.annotations.Test;

import io.github.dmlloyd.classfile.AccessFlags;
import io.github.dmlloyd.classfile.Attributes;
import io.github.dmlloyd.classfile.MethodModel;
import io.github.dmlloyd.classfile.attribute.ExceptionsAttribute;
import io.github.dmlloyd.classfile.extras.reflect.AccessFlag;

public class TestMethodSignature extends CodeModelTest {

    @Test
    public void testCodeFollowingTrueIfWithReturnIsRemoved () {
	MethodModel method =
	    getMethodModel ("C.java", "import java.io.*; class C { public static void a (boolean b) throws IOException { " +
			    "if (b) throw new IOException (); }}", "C", "a");
	// TODO: verify that we have a throws clause
	AccessFlags af = method.flags ();
	assert af.has (AccessFlag.PUBLIC);
	assert af.has (AccessFlag.STATIC);
	MethodTypeDesc mtd = method.methodTypeSymbol ();
	assert mtd.returnType ().equals (ConstantDescs.CD_void);
	assert mtd.parameterCount () == 1;
	assert mtd.parameterType (0).equals (ConstantDescs.CD_boolean);

	List<ExceptionsAttribute> ls = method.findAttributes (Attributes.EXCEPTIONS);
	assert ls.size () == 1 : "Failed to find correct exceptions";
    }
}
