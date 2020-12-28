package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class Assignment extends SyntaxTreeNode {
    private ParseTreeNode left;
    private Token operator;
    private ParseTreeNode right;

    public Assignment (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	left = children.get (0);
	operator = ((TokenNode)children.get (1)).getToken ();
	right = children.get (2);
    }

    @Override public Object getValue() {
	return left + " " + operator.getName () + " " + right.getValue ();
    }
}
