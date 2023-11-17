package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class IntLiteral extends TokenNode implements NumericLiteral {
    private final int value;

    public IntLiteral (Token token, int value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public double doubleValue () {
	return value;
    }

    @Override public float floatValue () {
	return value;
    }

    @Override public int intValue () {
	return value;
    }

    @Override public long longValue () {
	return value;
    }

    @Override public Integer getValue () {
	return value;
    }
}
