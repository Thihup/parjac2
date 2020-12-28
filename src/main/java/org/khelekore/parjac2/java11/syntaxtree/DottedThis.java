package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DottedThis extends SyntaxTreeNode {
    private final ParseTreeNode type;

    public DottedThis (ParseTreeNode type) {
	super (type.getPosition ());
	this.type = type;
    }

    @Override public Object getValue () {
	return type + ".this";
    }
}
