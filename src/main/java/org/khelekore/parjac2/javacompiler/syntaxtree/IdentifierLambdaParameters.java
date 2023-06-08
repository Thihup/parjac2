package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class IdentifierLambdaParameters extends LambdaParameters {
    private final String id;
    public IdentifierLambdaParameters (ParseTreeNode n, String id) {
	super (n.getPosition ());
	this.id = id;
    }

    @Override public Object getValue () {
	return id;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// nothing
    }
}
