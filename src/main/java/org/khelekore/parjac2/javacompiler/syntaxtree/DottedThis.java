package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
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

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (type);
    }
}
