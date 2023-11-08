package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class IfThenStatement extends SyntaxTreeNode {
    private final ParseTreeNode exp;
    private final ParseTreeNode thenPart;
    private final ParseTreeNode elsePart;

    public IfThenStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	exp = children.get (2);
	thenPart = children.get (4);
	elsePart = rule.size () > 6 ? children.get (6) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("if (").append (exp).append (") ").append (thenPart);
	if (elsePart != null)
	    sb.append (" else ").append (elsePart);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (exp);
	v.accept (thenPart);
	if (elsePart != null)
	    v.accept (elsePart);
    }

    public ParseTreeNode test () {
	return exp;
    }

    public ParseTreeNode thenPart () {
	return thenPart;
    }

    public ParseTreeNode elsePart () {
	return elsePart;
    }

    public boolean hasElse () {
	return elsePart != null;
    }
}
