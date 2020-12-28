package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CatchClause extends SyntaxTreeNode {
    private final CatchFormalParameter param;
    private final Block block;

    public CatchClause (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	param = (CatchFormalParameter)children.get (2);
	block = (Block)children.get (4);
    }

    @Override public Object getValue () {
	return "catch (" + param + ")" + block;
    }
}
