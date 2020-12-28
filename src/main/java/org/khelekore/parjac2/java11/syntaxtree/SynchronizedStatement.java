package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SynchronizedStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression;
    private final Block block;

    public SynchronizedStatement (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	expression = children.get (2);
	block = (Block)children.get (4);
    }

    @Override public Object getValue () {
	return "synchronized (" + expression + ")" + block;
    }
}
