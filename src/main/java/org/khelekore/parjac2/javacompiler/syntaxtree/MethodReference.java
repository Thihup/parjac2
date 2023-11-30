package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.javacompiler.MethodInfo;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class MethodReference extends SyntaxTreeNode {
    private FullNameHandler type;
    // Runnable r = this::foo, calling = Runnable, actual = foo
    private MethodInfo calling;
    private MethodInfo actual;

    public MethodReference (ParsePosition pos) {
	super (pos);
    }

    public FullNameHandler type () {
	return type;
    }

    public MethodInfo methodInfo () {
	return calling;
    }

    public MethodInfo actualMethod () {
	return actual;
    }

    public void type (FullNameHandler type, MethodInfo calling, MethodInfo actual) {
	this.type = type;
	this.calling = calling;
	this.actual = actual;
    }

    /** Get thet name of the method we are trying to reference */
    public abstract String name ();

    // the the thing the method reference is acting on, the thing before the ::, so something like "this", "anInstance" or "AClassName"
    public abstract ParseTreeNode on ();
}
