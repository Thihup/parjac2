package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SimpleResource extends SyntaxTreeNode {
    private final ParseTreeNode t; // ExpressionName or FieldAccess
    public SimpleResource (ParseTreeNode t) {
	super (t.getPosition ());
	this.t = t;
    }

    @Override public Object getValue () {
	return t.getValue ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (t);
    }
}
