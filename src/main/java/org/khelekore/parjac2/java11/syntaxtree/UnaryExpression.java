package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class UnaryExpression extends SyntaxTreeNode {
    private Token operator;
    private ParseTreeNode exp;
    public UnaryExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	operator = ((TokenNode)children.get (0)).getToken ();
	exp = children.get (1);
    }

    @Override public Object getValue() {
	return operator.toString () + exp;
    }
}
