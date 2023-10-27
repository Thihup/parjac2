package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SynchronizedStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression;
    private final Block block;

    public SynchronizedStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	expression = children.get (2);
	block = (Block)children.get (4);
    }

    @Override public Object getValue () {
	return "synchronized (" + expression + ")" + block;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (expression);
	v.accept (block);
    }
}
