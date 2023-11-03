package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class TwoPartExpression extends SyntaxTreeNode {
    private final ParseTreeNode part1;
    private final TokenNode operator;
    private final ParseTreeNode part2;
    private FullNameHandler type;

    public TwoPartExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	part1 = children.get (0);
	operator = (TokenNode)children.get (1);
	part2 = children.get (2);
    }

    @Override public Object getValue() {
	return part1 + " " + operator + " " + part2;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (part1);
	v.accept (operator);
	v.accept (part2);
    }

    public void type (FullNameHandler type) {
	this.type = type;
    }

    public FullNameHandler type () {
	return type;
    }

    public ParseTreeNode part1 () {
	return part1;
    }

    public Token token () {
	return operator.token ();
    }

    public ParseTreeNode part2 () {
	return part2;
    }
}
