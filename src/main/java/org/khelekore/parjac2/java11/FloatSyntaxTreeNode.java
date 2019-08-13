package org.khelekore.parjac2.java11;

import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenSyntaxTreeNode;

public class FloatSyntaxTreeNode extends TokenSyntaxTreeNode {
    private final float value;

    public FloatSyntaxTreeNode (Token token, float value) {
	super (token);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }
}