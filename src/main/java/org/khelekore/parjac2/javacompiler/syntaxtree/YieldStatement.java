package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class YieldStatement extends SyntaxTreeNode {
    private ParseTreeNode expression;

    public YieldStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	expression = children.get (1);
    }

    public ParseTreeNode expression () {
	return expression;
    }

    @Override public Object getValue () {
	return "yield " + expression.getValue () + ";";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (expression);
    }
}
