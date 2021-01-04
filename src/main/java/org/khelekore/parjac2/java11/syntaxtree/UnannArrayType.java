package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class UnannArrayType extends SyntaxTreeNode {
    private ParseTreeNode type;
    private Dims dims;

    public UnannArrayType (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	type = children.get (0);
	dims = (Dims)children.get (1);
    }

    @Override public Object getValue() {
	return type.toString () + dims;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (type);
	v.accept (dims);
    }
}
