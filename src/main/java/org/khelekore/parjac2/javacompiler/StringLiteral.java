package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class StringLiteral extends TokenNode {
    private final String value;

    public StringLiteral (Token token, String value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }
}
