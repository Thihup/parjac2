package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayAccess extends SyntaxTreeNode {
    private final ParseTreeNode from;
    private final ParseTreeNode expression;

    public ArrayAccess (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	from = children.get (0);
	expression = children.get (2);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (from).append ("[").append (expression).append ("]");
	return sb.toString ();
    }
}
