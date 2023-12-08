package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.PrimitiveType;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

/** Interface with methods used to find fields or local variables */
public interface VariableInfo {
    public enum Type { FIELD, PARAMETER, LOCAL, ARRAY_LENGTH }

    Type fieldType ();

    int flags ();

    String name ();

    default ClassDesc typeClassDesc () {
	return ClassDescUtils.getClassDesc (typeName ());
    }

    FullNameHandler typeName ();

    static FullNameHandler typeToFullName (ParseTreeNode p) {
	if (p instanceof PrimitiveType pt) {
	    return pt.fullName ();
	}
	if (p instanceof ClassType ct) {
	    return ct.fullName ();
	}
	if (p instanceof ArrayType at) {
	    return FullNameHelper.type (at);
	}
	throw new IllegalStateException ("Unhandled type: " + p.getClass ().getName () + ": " + p + ", position: " + p.position ());
    }
}
