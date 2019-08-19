package org.khelekore.parjac2.java11;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenSyntaxTreeNode;

public class StringSyntaxTreeNode extends TokenSyntaxTreeNode {
    private final String value;

    public StringSyntaxTreeNode (Token token, String value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }
}