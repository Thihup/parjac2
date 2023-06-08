package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;

public class ClassType extends SyntaxTreeNode {
    private final List<SimpleClassType> types;

    public ClassType (ParsePosition pos, List<SimpleClassType> types) {
	super (pos);
	this.types = types;
    }

    @Override public Object getValue () {
	return types.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	types.forEach (v::accept);
    }

    public List<SimpleClassType> getTypes () {
	return types;
    }
}
