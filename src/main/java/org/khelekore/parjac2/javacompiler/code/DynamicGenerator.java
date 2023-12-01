package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;

import io.github.dmlloyd.classfile.CodeBuilder;

public class DynamicGenerator {

    public static void callDynamic (CodeBuilder cb, FullNameHandler currentClass, int currentMethodFlags,
				    String dynamicMethod, MethodTypeDesc mtd,
				    FullNameHandler dynamicType, boolean forceStatic) {
	DirectMethodHandleDesc.Kind kind = DirectMethodHandleDesc.Kind.STATIC;
	ClassDesc owner = ClassDesc.ofInternalName ("java/lang/invoke/LambdaMetafactory");
	String name = "metafactory";
	MethodTypeDesc lookupMethodType =
	    MethodTypeDesc.ofDescriptor ("(" +
					 "Ljava/lang/invoke/MethodHandles$Lookup;" +  // caller
					 "Ljava/lang/String;" +                       // interface method name
					 "Ljava/lang/invoke/MethodType;" +            // factoryType
					 "Ljava/lang/invoke/MethodType;" +            // interfaceMethodType
					 "Ljava/lang/invoke/MethodHandle;" +          // implementation
					 "Ljava/lang/invoke/MethodType;" +            // dynamicMethodType
					 ")" +
					 "Ljava/lang/invoke/CallSite;");
	DirectMethodHandleDesc bootstrapMethod =
	    MethodHandleDesc.ofMethod (kind, owner, name, lookupMethodType);
	ClassDesc lambdaOwner = ClassDescUtils.getClassDesc (currentClass);

	DirectMethodHandleDesc.Kind dmk = DirectMethodHandleDesc.Kind.STATIC;
	List<ClassDesc> types = List.of ();
	if (!forceStatic && !Flags.isStatic (currentMethodFlags)) {
	    dmk = DirectMethodHandleDesc.Kind.VIRTUAL;
	    cb.aload (0);
	    types = List.of (lambdaOwner);
	}
	ClassDesc ret = ClassDescUtils.getClassDesc (dynamicType);
	MethodTypeDesc invocationType = MethodTypeDesc.of (ret, types);
	MethodHandleDesc mhd = MethodHandleDesc.of (dmk, lambdaOwner, dynamicMethod, mtd.descriptorString ());
	ConstantDesc[] bootstrapArgs = {mtd, mhd, mtd}; // TODO: second mtd may require changes
	DynamicCallSiteDesc ref = DynamicCallSiteDesc.of (bootstrapMethod, "run", invocationType, bootstrapArgs);
	cb.invokedynamic (ref);
    }
}
