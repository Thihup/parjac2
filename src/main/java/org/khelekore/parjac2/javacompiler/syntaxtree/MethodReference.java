package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.javacompiler.MethodInfo;
import org.khelekore.parjac2.parser.ParsePosition;

public abstract class MethodReference extends SyntaxTreeNode {
    private FullNameHandler type;
    private MethodInfo mi;

    public MethodReference (ParsePosition pos) {
	super (pos);
    }

    public FullNameHandler type () {
	return type;
    }

    public MethodInfo methodInfo () {
	return mi;
    }

    public void type (FullNameHandler type, MethodInfo mi) {
	this.type = type;
	this.mi = mi;
    }

    /** Get thet name of the method we are trying to reference */
    public abstract String name ();
}
