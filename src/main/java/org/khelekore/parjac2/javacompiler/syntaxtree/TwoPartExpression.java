package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class TwoPartExpression extends SyntaxTreeNode {
    private final ParseTreeNode part1;
    private final Token operator;
    private final ParseTreeNode part2;
    private FullNameHandler type;
    private OpType optype;

    public enum OpType { PRIMITIVE_OP, STRING_OP, OBJECT_OP }

    public TwoPartExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	part1 = children.get (0);
	operator = ((TokenNode)children.get (1)).token ();
	part2 = children.get (2);
    }

    public TwoPartExpression (ParseTreeNode part1, Token operator, ParseTreeNode part2) {
	super (part1.position ());
	this.part1 = part1;
	this.operator = operator;
	this.part2 = part2;
    }

    @Override public Object getValue () {
	return part1 + " " + operator.getName () + " " + part2;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (part1);
	v.accept (part2);
    }

    public void fullName (FullNameHandler type) {
	this.type = type;
    }

    public FullNameHandler fullName () {
	return type;
    }

    public void optype (OpType optype) {
	this.optype = optype;
    }

    public OpType optype () {
	return optype;
    }

    public ParseTreeNode part1 () {
	return part1;
    }

    public Token token () {
	return operator;
    }

    public ParseTreeNode part2 () {
	return part2;
    }
}
