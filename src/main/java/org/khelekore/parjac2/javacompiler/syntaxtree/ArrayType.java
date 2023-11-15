package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayType extends SyntaxTreeNode {
    private ParseTreeNode type;
    private Dims dims;

    public ArrayType (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	type = children.get (0); // primitive or class type
	dims = (Dims)children.get (1);
    }

    public ArrayType (ClassType ct, int rank) {
	super (ct.position ());
	type = ct;
	dims = new Dims (ct.position (), 1);
    }

    public ArrayType (PrimitiveType pt, int rank) {
	super (pt.position ());
	type = pt;
	dims = new Dims (pt.position (), 1);
    }

    @Override public Object getValue () {
	return type.toString () + dims;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (type);
	v.accept (dims);
    }

    // may be primitive type or class type
    public ParseTreeNode getType () {
	return type;
    }

    public Dims getDims () {
	return dims;
    }

    public int rank () {
	return dims.rank ();
    }
}
