package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DoStatement extends SyntaxTreeNode {
    private final ParseTreeNode statement;
    private final ParseTreeNode expression;

    public DoStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	statement = children.get (1);
	expression = children.get (4);
    }

    public ParseTreeNode expression () {
	return expression;
    }

    public ParseTreeNode statement () {
	return statement;
    }

    @Override public Object getValue () {
	return "do " + statement + " while (" + expression + ");";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (statement);
	v.accept (expression);
    }
}
