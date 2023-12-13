package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Dims;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterList;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.PrimitiveType;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ClassDescUtils {
    public static ClassDesc getParseTreeClassDesc (ParseTreeNode type) {
	ClassDesc desc = switch (type) {
	case PrimitiveType pt -> getClassDesc (pt);
	case ClassType ct -> getClassDesc (ct);
	case ArrayType at -> getClassDesc (at);
	case TokenNode tn when tn.token ().getName ().equals ("void") -> ConstantDescs.CD_void;
	default -> throw new IllegalStateException ("BytecodeGenerator: Unhandled field type: " + type.getClass ().getName () + ": " + type);
	};
	return desc;
    }

    public static ClassDesc getClassDesc (FullNameHandler fn) {
	if (fn.isArray ()) {
	    FullNameHandler.ArrayHandler a = (FullNameHandler.ArrayHandler)fn;
	    return getClassDesc (a.fn ()).arrayType (a.rank ());
	}

	if (fn.isPrimitive ())
	    return ClassDesc.ofDescriptor (((FullNameHandler.Primitive)fn).getSignature ());
	return ClassDesc.of (fn.getFullDollarName ());
    }

    public static ClassDesc getClassDesc (PrimitiveType pt) {
	String descriptor = pt.fullName ().getSignature ();
	return ClassDesc.ofDescriptor (descriptor);
    }

    public static ClassDesc getClassDesc (ClassType ct) {
	if (ct == null)
	    return ConstantDescs.CD_Object; // common for super classes to be null
	return getClassDesc (ct.fullName ());
    }

    public static ClassDesc getClassDesc (ArrayType at) {
	ParseTreeNode type = at.getType ();
	ClassDesc base = getParseTreeClassDesc (type);
	Dims dims = at.getDims ();
	int rank = dims.rank ();
	return base.arrayType (rank);
    }

    public static MethodTypeDesc methodTypeDesc (FormalParameterList ls, FullNameHandler result) {
	List<FormalParameterBase> params = ls == null ? List.of () : ls.getParameters ();
	return methodTypeDescInternal (result, toFullNames (params));
    }

    private static List<FullNameHandler> toFullNames (List<FormalParameterBase> ls) {
	return ls.stream ().map (fpb -> FullNameHelper.type (fpb)).toList ();
    }

    public static MethodTypeDesc methodTypeDesc (FullNameHandler result, FullNameHandler... params) {
	return methodTypeDescInternal (result, List.of (params));
    }

    private static MethodTypeDesc methodTypeDescInternal (FullNameHandler result, Iterable<FullNameHandler> params) {
	List<ClassDesc> paramCds = new ArrayList<> ();
	if (params != null) {
	    for (FullNameHandler fn : params)
		paramCds.add (getClassDesc (fn));
	}
	ClassDesc resultCd = getClassDesc (result);
	return MethodTypeDesc.of (resultCd, paramCds);
    }
}
