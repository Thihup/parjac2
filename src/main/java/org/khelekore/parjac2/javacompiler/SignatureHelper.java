package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterList;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameters;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SignatureHelper {
    public static MethodSignatureHolder getMethodSignature (ClassInformationProvider cip, GenericTypeHelper genericTypeHelper,
							    TypeParameters tps, FormalParameterList params, ParseTreeNode result) {
	StringBuilder sb = new StringBuilder ();
	boolean foundGenericTypes = false;
	if (tps != null) {
	    foundGenericTypes = true;
	    genericTypeHelper.appendTypeParametersSignature (sb, tps, cip, false);
	}

	List<ClassDesc> paramDescs = List.of ();
	sb.append ("(");
	if (params != null) {
	    paramDescs = new ArrayList<> (params.size ());
	    for (FormalParameterBase fp : params.getParameters ()) {
		ParseTreeNode p = fp.type ();
		foundGenericTypes |= hasGenericType (p);
		paramDescs.add (ClassDescUtils.getParseTreeClassDesc (p));
		sb.append (genericTypeHelper.getGenericType (p, cip, true));
	    }
	}
	sb.append (")");

	foundGenericTypes |= hasGenericType (result);
	ClassDesc returnDesc = ClassDescUtils.getParseTreeClassDesc (result);
	sb.append (genericTypeHelper.getGenericType (result, cip, true));

	MethodTypeDesc descriptor = MethodTypeDesc.of (returnDesc, paramDescs.toArray (new ClassDesc[paramDescs.size ()]));

	String signature = foundGenericTypes ? sb.toString () : null;
	return new MethodSignatureHolder (descriptor, signature);
    }

    public static boolean hasGenericType (List<ClassType> ls) {
	return ls != null && ls.stream ().anyMatch (SignatureHelper::hasGenericType);
    }

    public static boolean hasGenericType (ParseTreeNode p) {
	return p instanceof ClassType ct && hasGenericType (ct);
    }

    public static boolean hasGenericType (ClassType ct) {
	return ct != null && (ct.fullName ().hasGenericType () || ct.getTypeParameter () != null);
    }

    public record MethodSignatureHolder (MethodTypeDesc desc, String signature) {
	// empty
    }
}
