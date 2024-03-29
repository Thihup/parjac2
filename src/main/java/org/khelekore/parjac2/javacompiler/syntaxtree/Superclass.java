package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Superclass extends SyntaxTreeNode {
    private ClassType type;
    public Superclass (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	this.type = (ClassType)children.get (1);
    }

    @Override public Object getValue () {
	return "extends " + type;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (type);
    }

    public ClassType getType () {
	return type;
    }
}
