package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ExpressionStatement extends SyntaxTreeNode {
    private ParseTreeNode statementExpression;
    public ExpressionStatement (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	statementExpression = children.get (0);
    }

    @Override public Object getValue () {
	return statementExpression + ";";
    }
}
