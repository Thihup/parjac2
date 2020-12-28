package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class PostDecrementExpression extends SyntaxTreeNode {
    private ParseTreeNode expression;
    public PostDecrementExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	expression = children.get (0);
    }

    @Override public Object getValue() {
	return expression + "--";
    }
}

