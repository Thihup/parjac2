package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class StaticInitializer extends SyntaxTreeNode {
    private final Block block;
    public StaticInitializer (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	block = (Block)children.get (1);
    }

    @Override public Object getValue () {
	return "static " + block;
    }
}
