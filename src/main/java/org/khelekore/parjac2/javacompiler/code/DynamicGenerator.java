package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.RecordDeclaration;

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

    public static void callObjectMethods (CodeBuilder cb, RecordDeclaration rd, FullNameHandler fn,
					  String methodName, FullNameHandler returnType, FullNameHandler... args) {
	DirectMethodHandleDesc.Kind kind = DirectMethodHandleDesc.Kind.STATIC;
	ClassDesc owner = ClassDesc.ofInternalName ("java/lang/runtime/ObjectMethods");
	String name = "bootstrap";
	MethodTypeDesc lookupMethodType =
	    MethodTypeDesc.ofDescriptor ("(" +
					 "Ljava/lang/invoke/MethodHandles$Lookup;" +
					 "Ljava/lang/String;" +
					 "Ljava/lang/invoke/TypeDescriptor;" +
					 "Ljava/lang/Class;" +
					 "Ljava/lang/String;" +
					 "[Ljava/lang/invoke/MethodHandle;" +
					 ")Ljava/lang/Object;");
	DirectMethodHandleDesc bootstrapMethod =
	    MethodHandleDesc.ofMethod (kind, owner, name, lookupMethodType);
	ClassDesc classDesc = ClassDescUtils.getClassDesc (fn);

	cb.aload (0);

	List<ClassDesc> argTypes = new ArrayList<> ();
	argTypes.add (classDesc);
	for (int i = 0; i < args.length; i++) {
	    argTypes.add (ClassDescUtils.getClassDesc (args[i]));
	    cb.loadLocal (FullNameHelper.getTypeKind (args[i]), i + 1);
	}
	MethodTypeDesc invocationType = MethodTypeDesc.of (ClassDescUtils.getClassDesc (returnType), argTypes);
	String fields = getAllFields (rd);
	List<MethodHandleDesc> getters = getGetters (rd, classDesc);
	ConstantDesc[] bootstrapArgs = new ConstantDesc[2 + getters.size ()];
	bootstrapArgs[0] = classDesc;
	bootstrapArgs[1] = fields;
	for (int i = 0; i < getters.size (); i++)
	    bootstrapArgs[i + 2] = getters.get (i);

	DynamicCallSiteDesc ref = DynamicCallSiteDesc.of (bootstrapMethod, methodName, invocationType, bootstrapArgs);
	cb.invokedynamic (ref);
    }

    private static String getAllFields (RecordDeclaration rd) {
	return String.join (";", rd.getFields ().keySet ());
    }

    private static List<MethodHandleDesc> getGetters (RecordDeclaration rd, ClassDesc owner) {
	List<MethodHandleDesc> ret = new ArrayList<> ();
	rd.getFields ().forEach ((name, fi) -> {
	    ret.add (MethodHandleDesc.ofField (DirectMethodHandleDesc.Kind.GETTER, owner, name,
					       ClassDescUtils.getClassDesc (FullNameHelper.type (fi.type ()))));
	    });
	return ret;
    }
}
