package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

/** Interface with methods used to find fields or local variables */
public interface VariableInfo {
    int flags ();

    String name ();

    ParseTreeNode type ();

    default FullNameHandler typeName () {
	ParseTreeNode p = type ();
	if (p instanceof TokenNode) {
	    // TODO: we should probably set ExpressionType or someting?
	}
	if (p instanceof ClassType ct) {
	    return ct.getFullNameHandler ();
	}
	throw new IllegalStateException ("Unhandled type: " + p.getClass ().getName () + ": " + p + ", position: " + p.position ());
    }
}
