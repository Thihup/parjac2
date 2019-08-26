package org.khelekore.parjac2.java11;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class Identifier extends TokenNode {
    private final String value;

    public Identifier (Token token, String value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public String getValue () {
	return value;
    }
}