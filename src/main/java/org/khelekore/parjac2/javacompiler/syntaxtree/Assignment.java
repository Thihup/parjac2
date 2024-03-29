package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class Assignment extends SyntaxTreeNode {
    private ParseTreeNode left;
    private Token operator;
    private ParseTreeNode right;

    public Assignment (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	left = children.get (0);
	operator = ((TokenNode)children.get (1)).token ();
	right = children.get (2);
    }

    public Assignment (ParseTreeNode left, Token operator, ParseTreeNode right) {
	super (left.position ());
	this.left = left;
	this.operator = operator;
	this.right = right;
    }

    @Override public Object getValue () {
	return left + " " + operator.getName () + " " + right.getValue ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (left);
	v.accept (right);
    }

    public FullNameHandler fullName () {
	return FullNameHelper.type (left);
    }

    public ParseTreeNode lhs () {
	return left;
    }

    public Token operator () {
	return operator;
    }

    public ParseTreeNode rhs () {
	return right;
    }
}
