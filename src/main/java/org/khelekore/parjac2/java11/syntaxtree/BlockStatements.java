package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class BlockStatements extends SyntaxTreeNode {
    private List<ParseTreeNode> statements;
    public BlockStatements (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	statements = new ArrayList<> ();
	statements.add (children.get (0));
	if (rule.size () > 1)
	    statements.addAll (((Multiple)children.get (1)).get ());
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	statements.forEach (s -> sb.append (s).append ("\n"));
	return sb.toString ();
    }
}
