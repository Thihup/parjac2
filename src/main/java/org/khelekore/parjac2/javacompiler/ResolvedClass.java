package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;

public class ResolvedClass {
    public final FullNameHandler type;
    public final TypeParameter tp;

    public ResolvedClass (FullNameHandler type) {
	this.type = type;
	this.tp = null;
    }

    public ResolvedClass (TypeParameter tp) {
	this.type = tp.getExpressionType ().getFullNameHandler ();
	this.tp = tp;
    }

    @Override public String toString () {
	return "ResolvedClass{type: " + type.getFullDollarName () + ", tp: " + tp + "}";
    }
}
