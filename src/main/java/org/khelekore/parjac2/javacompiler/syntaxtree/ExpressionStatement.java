package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ExpressionStatement extends SyntaxTreeNode {
    private ParseTreeNode statementExpression;

    public ExpressionStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	statementExpression = children.get (0);
    }

    @Override public Object getValue () {
	return statementExpression + ";";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (statementExpression);
    }
}
