package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class Identifier extends TokenNode {
    private final String value;

    /**
     * @param token the type of identifier this is, typically Identifier or TypeIdentifier
     */
    public Identifier (Token token, String value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public String getValue () {
	return value;
    }

    @Override public String toString () {
	return value;
    }
}
