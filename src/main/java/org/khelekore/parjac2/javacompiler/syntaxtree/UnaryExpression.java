package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class UnaryExpression extends SyntaxTreeNode {
    private final Token operator;
    private final ParseTreeNode exp;

    public UnaryExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	operator = ((TokenNode)children.get (0)).token ();
	exp = children.get (1);
    }

    @Override public Object getValue () {
	return operator.toString () + exp;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (exp);
    }

    public Token operator () {
	return operator;
    }

    public ParseTreeNode expression () {
	return exp;
    }
}
