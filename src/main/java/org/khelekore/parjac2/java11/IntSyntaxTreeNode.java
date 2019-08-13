package org.khelekore.parjac2.java11;

import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenSyntaxTreeNode;

public class IntSyntaxTreeNode extends TokenSyntaxTreeNode {
    private final int value;

    public IntSyntaxTreeNode (Token token, int value) {
	super (token);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }
}