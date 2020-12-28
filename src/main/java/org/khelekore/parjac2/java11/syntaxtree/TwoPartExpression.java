package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class TwoPartExpression extends SyntaxTreeNode {
    private ParseTreeNode part1;
    private TokenNode operator;
    private ParseTreeNode part2;
    public TwoPartExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	part1 = children.get (0);
	operator = (TokenNode)children.get (1);
	part2 = children.get (2);
    }

    @Override public Object getValue() {
	return part1 + " " + operator + " " + part2;
    }
}
