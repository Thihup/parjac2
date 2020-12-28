package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class PreDecrementExpression extends SyntaxTreeNode {
    private ParseTreeNode expression;
    public PreDecrementExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	expression = children.get (1);
    }

    @Override public Object getValue() {
	return expression + "--";
    }
}
