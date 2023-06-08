package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ThisPrimary extends SyntaxTreeNode {
    public ThisPrimary (ParseTreeNode n) {
	super (n.getPosition ());
    }

    @Override public Object getValue () {
	return "this";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// nothing
    }
}
