package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Ternary extends SyntaxTreeNode {
    private ParseTreeNode test;
    private ParseTreeNode thenPart;
    private ParseTreeNode elsePart;
    public Ternary (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	test = children.get (0);
	thenPart = children.get (2);
	elsePart = children.get (4);
    }
    @Override public Object getValue () {
	return test + " ? " + thenPart + " : " + elsePart;
    }
}
