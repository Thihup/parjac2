package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Ternary extends SyntaxTreeNode {
    private final ParseTreeNode test;
    private final ParseTreeNode thenPart;
    private final ParseTreeNode elsePart;
    private FullNameHandler type;

    public Ternary (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	test = children.get (0);
	thenPart = children.get (2);
	elsePart = children.get (4);
    }

    public void type (FullNameHandler type) {
	this.type = type;
    }

    public FullNameHandler type () {
	return type;
    }

    @Override public Object getValue () {
	return test + " ? " + thenPart + " : " + elsePart;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (test);
	v.accept (thenPart);
	v.accept (elsePart);
    }

    public ParseTreeNode thenPart () {
	return thenPart;
    }

    public ParseTreeNode elsePart () {
	return thenPart;
    }
}
