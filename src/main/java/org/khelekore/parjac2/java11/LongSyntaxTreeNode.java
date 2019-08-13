package org.khelekore.parjac2.java11;

import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenSyntaxTreeNode;

public class LongSyntaxTreeNode extends TokenSyntaxTreeNode {
    private final long value;

    public LongSyntaxTreeNode (Token token, long value) {
	super (token);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }
}