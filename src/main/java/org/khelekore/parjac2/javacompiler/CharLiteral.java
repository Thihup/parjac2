package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class CharLiteral extends TokenNode {
    private final char value;

    public CharLiteral (Token token, char value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    public char charValue () {
	return value;
    }

    @Override public Object getValue () {
	return value;
    }
}
