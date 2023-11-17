package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class LongLiteral extends TokenNode {
    private final long value;

    public LongLiteral (Token token, long value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }

    public long longValue () {
	return value;
    }
}
