package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ThisPrimary extends SyntaxTreeNode {
    private FullNameHandler type;

    public ThisPrimary (ParseTreeNode n) {
	super (n.position ());
    }

    public ThisPrimary (ParseTreeNode p, FullNameHandler type) {
	super (p.position ());
	this.type = type;
    }

    public void type (FullNameHandler type) {
	this.type = type;
    }

    public FullNameHandler type () {
	return type;
    }

    @Override public Object getValue () {
	return "this";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// nothing
    }
}
