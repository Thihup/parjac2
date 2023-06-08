package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parsetree.NodeVisitor;

public class UnannClassType extends SyntaxTreeNode {
    private List<SimpleClassType> types;

    public UnannClassType (SimpleClassType sct) {
	super (sct.getPosition ());
	this.types = new ArrayList<> ();
	types.add (sct);
    }

    @Override public Object getValue() {
	return types.toString ();
    }

    public void add (SimpleClassType sct) {
	types.add (sct);
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	types.forEach (v::accept);
    }
}
