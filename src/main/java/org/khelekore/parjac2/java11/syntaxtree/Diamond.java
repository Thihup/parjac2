package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public class Diamond extends SyntaxTreeNode {
    public Diamond (ParsePosition pos) {
	super (pos);
    }

    @Override public Object getValue () {
	return "<>";
    }
}
