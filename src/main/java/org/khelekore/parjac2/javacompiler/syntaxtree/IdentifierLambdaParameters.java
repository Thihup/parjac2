package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class IdentifierLambdaParameters extends LambdaParameters {
    private final String id;
    public IdentifierLambdaParameters (ParseTreeNode n, String id) {
	super (n.position ());
	this.id = id;
    }

    @Override public Object getValue () {
	return id;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// nothing
    }

    @Override public int numberOfArguments () {
	return 1;
    }

    @Override public String parameterName (int i) {
	return id;
    }

    @Override public FullNameHandler parameter (int i) {
	return null; // unknown
    }
}
