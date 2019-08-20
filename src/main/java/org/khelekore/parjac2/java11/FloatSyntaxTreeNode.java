package org.khelekore.parjac2.java11;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class FloatSyntaxTreeNode extends TokenNode {
    private final float value;

    public FloatSyntaxTreeNode (Token token, float value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }
}