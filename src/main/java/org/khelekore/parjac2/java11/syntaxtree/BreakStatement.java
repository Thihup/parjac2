package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class BreakStatement extends SyntaxTreeNode {
    private final String id;

    public BreakStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	id = rule.size () > 2 ? ((Identifier)children.get (1)).getValue () : null;
    }

    @Override public Object getValue () {
	return id != null ? "break " + id + ";" : "break;";
    }
}
