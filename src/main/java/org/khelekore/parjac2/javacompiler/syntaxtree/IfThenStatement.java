package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class IfThenStatement extends SyntaxTreeNode {
    private final ParseTreeNode exp;
    private final ParseTreeNode ifStatement;
    private final ParseTreeNode elseStatement;

    public IfThenStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	exp = children.get (2);
	ifStatement = children.get (4);
	elseStatement = rule.size () > 6 ? children.get (6) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("if (").append (exp).append (") ").append (ifStatement);
	if (elseStatement != null)
	    sb.append (" else ").append (elseStatement);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (exp);
	v.accept (ifStatement);
	if (elseStatement != null)
	    v.accept (elseStatement);
    }
}
