package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Block extends SyntaxTreeNode {
    private final BlockStatements statements;
    public Block (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	statements = (rule.size () > 2) ? (BlockStatements)children.get (1) : null;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{\n");
	if (statements != null)
	    sb.append (statements);
	sb.append ("}");
	return sb.toString ();
    }
}
