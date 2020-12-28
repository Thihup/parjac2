package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ThisPrimary extends SyntaxTreeNode {
    public ThisPrimary (ParseTreeNode n) {
	super (n.getPosition ());
    }

    @Override public Object getValue () {
	return "this";
    }
}
