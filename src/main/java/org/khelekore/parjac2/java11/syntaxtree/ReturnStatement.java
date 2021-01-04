package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ReturnStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression;

    public ReturnStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	expression = rule.size () > 2 ? children.get (1) : null;
    }

    @Override public Object getValue () {
	return expression != null ? "return " + expression.getValue () + ";" : "return;";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (expression != null)
	    v.accept (expression);
    }
}
