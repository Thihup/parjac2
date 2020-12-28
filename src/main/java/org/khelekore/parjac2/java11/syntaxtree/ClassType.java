package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;

public class ClassType extends SyntaxTreeNode {
    private final List<SimpleClassType> types;

    public ClassType (ParsePosition pos, List<SimpleClassType> types) {
	super (pos);
	this.types = types;
    }

    @Override public Object getValue () {
	return types.toString ();
    }

    public List<SimpleClassType> getTypes () {
	return types;
    }
}
