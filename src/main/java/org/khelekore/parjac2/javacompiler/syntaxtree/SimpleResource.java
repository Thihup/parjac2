package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SimpleResource extends SyntaxTreeNode {
    private final ParseTreeNode t; // LocalVariableDeclaration or VariableAccess

    public SimpleResource (ParseTreeNode t) {
	super (t.position ());
	this.t = t;
    }

    public ParseTreeNode resource () {
	return t;
    }

    @Override public Object getValue () {
	return t.getValue ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (t);
    }
}
