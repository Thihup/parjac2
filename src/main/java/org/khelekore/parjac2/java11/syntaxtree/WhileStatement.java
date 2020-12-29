package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class WhileStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression;
    private final ParseTreeNode statement;

    public WhileStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	expression = children.get (2);
	statement = children.get (4);
    }

    @Override public Object getValue () {
	return "while (" + expression + ") " + statement;
    }
}
