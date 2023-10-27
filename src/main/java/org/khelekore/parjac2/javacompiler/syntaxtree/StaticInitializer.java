package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class StaticInitializer extends SyntaxTreeNode {
    private final Block block;
    public StaticInitializer (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	block = (Block)children.get (1);
    }

    @Override public Object getValue () {
	return "static " + block;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (block);
    }
}
