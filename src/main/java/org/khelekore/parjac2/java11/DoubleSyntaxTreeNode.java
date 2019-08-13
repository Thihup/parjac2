package org.khelekore.parjac2.java11;

import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenSyntaxTreeNode;

public class DoubleSyntaxTreeNode extends TokenSyntaxTreeNode {
    private final double value;

    public DoubleSyntaxTreeNode (Token token, double value) {
	super (token);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }
}