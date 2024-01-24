package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ContinueStatement extends SyntaxTreeNode {
    private final String id;

    public ContinueStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	id = rule.size () > 2 ? ((Identifier)children.get (1)).getValue () : null;
    }

    public String id () {
	return id;
    }

    @Override public Object getValue () {
	return id != null ? "continue " + id + ";" : "continue;";
    }

    @Override public void visitChildNodes(NodeVisitor v) {
	// nothing
    }
}
