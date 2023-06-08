package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Finally extends SyntaxTreeNode {
    private final Block block;

    public Finally (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	block = (Block)children.get (1);
    }

    @Override public Object getValue () {
	return "finally " + block;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (block);
    }
}
