package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayAccess extends SyntaxTreeNode {
    private final ParseTreeNode from;
    private final ParseTreeNode expression;

    public ArrayAccess (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	from = children.get (0);
	expression = children.get (2);
    }

    public FullNameHandler type () {
	return FullNameHelper.type (from).inner ();
    }

    public ParseTreeNode from () {
	return from;
    }

    public ParseTreeNode slot () {
	return expression;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (from).append ("[").append (expression).append ("]");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (from);
	v.accept (expression);
    }
}
