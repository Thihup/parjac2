package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class IntLiteral extends TokenNode {
    private final int value;

    public IntLiteral (Token token, int value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    public int intValue () {
	return value;
    }

    @Override public Integer getValue () {
	return value;
    }
}
