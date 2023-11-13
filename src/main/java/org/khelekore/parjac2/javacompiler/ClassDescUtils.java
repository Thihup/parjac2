package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;

import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Dims;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ClassDescUtils {
    public static ClassDesc getParseTreeClassDesc (ParseTreeNode type) {
	ClassDesc desc = switch (type) {
	case TokenNode tn -> getClassDesc (tn);
	case ClassType ct -> getClassDesc (ct);
	case ArrayType at -> getClassDesc (at);
	default -> throw new IllegalStateException ("BytecodeGenerator: Unhandled field type: " + type.getClass ().getName () + ": " + type);
	};
	return desc;
    }

    public static ClassDesc getClassDesc (FullNameHandler fn) {
	if (fn.isArray ()) {
	    FullNameHandler.ArrayHandler a = (FullNameHandler.ArrayHandler)fn;
	    return getClassDesc (a.fn ()).arrayType ();
	}

	if (fn.isPrimitive ())
	    return ClassDesc.ofDescriptor (((FullNameHandler.PrimitiveType)fn).getSignature ());
	return ClassDesc.of (fn.getFullDollarName ());
    }

    public static ClassDesc getClassDesc (TokenNode tn) {
	String descriptor = FullNameHelper.getPrimitive (tn).getSignature ();
	return ClassDesc.ofDescriptor (descriptor);
    }

    public static ClassDesc getClassDesc (ClassType ct) {
	if (ct == null)
	    return ConstantDescs.CD_Object; // common for super classes to be null
	return ClassDesc.of (ct.getFullDollarName ());
    }

    public static ClassDesc getClassDesc (ArrayType at) {
	ParseTreeNode type = at.getType ();
	ClassDesc base = getParseTreeClassDesc (type);
	Dims dims = at.getDims ();
	int rank = dims.rank ();
	return base.arrayType (rank);
    }
}
