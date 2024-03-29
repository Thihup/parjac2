package org.khelekore.parjac2.parser;

import java.util.Comparator;

public class ParsePosition {
    private int lineNumber;
    private int tokenColumn;
    private int tokenStartPos;
    private int tokenEndPos;

    public ParsePosition (int lineNumber, int tokenColumn,
			  int tokenStartPos, int tokenEndPos) {
	this.lineNumber = lineNumber;
	this.tokenColumn = tokenColumn;
	this.tokenStartPos = tokenStartPos;
	this.tokenEndPos = tokenEndPos;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{" +
	    "line: " + lineNumber +
	    ", column: " + tokenColumn +
	    ", token start pos: " + tokenStartPos +
	    ", token end pos: " + tokenEndPos +
	    "}";
    }

    public String toShortString () {
	return "(" + lineNumber + "," + tokenColumn + ")";
    }

    /** Get the start position, as in character position in totalinput, of the current token */
    public int getTokenStartPos () {
	return tokenStartPos;
    }

    /** Get the end position, as in character position in total input, of the current token */
    public int getTokenEndPos () {
	return tokenEndPos;
    }

    /** Get the line number of the current token */
    public int getLineNumber () {
	return lineNumber;
    }

    /** Get the start column of the current token */
    public int getTokenColumn () {
	return tokenColumn;
    }

    @Override public int hashCode () {
	return lineNumber << 16 | (0xffff & tokenColumn);
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o instanceof ParsePosition) {
	    ParsePosition pp = (ParsePosition)o;
	    return pp.lineNumber == lineNumber &&
		pp.tokenColumn == tokenColumn &&
		pp.tokenStartPos == tokenStartPos &&
		pp.tokenEndPos == tokenEndPos;
	}
	return false;
    }

    public static Comparator<ParsePosition> PositionComparator =
	Comparator
	.comparingInt (ParsePosition::getLineNumber)
	.thenComparingInt (ParsePosition::getTokenColumn);
}
