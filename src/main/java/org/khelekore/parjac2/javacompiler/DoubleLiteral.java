package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.TokenNode;

public class DoubleLiteral extends TokenNode implements NumericLiteral {
    private final double value;

    public DoubleLiteral (Token token, double value, ParsePosition pos) {
	super (token, pos);
	this.value = value;
    }

    @Override public Object getValue () {
	return value;
    }

    @Override public double doubleValue () {
	return value;
    }

    @Override public float floatValue () {
	return (float)value;
    }

    @Override public int intValue () {
	return (int)value;
    }

    @Override public long longValue () {
	return (long)value;
    }
}
