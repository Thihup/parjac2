package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class AssertStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression1;
    private final ParseTreeNode expression2;

    public AssertStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	expression1 = children.get (1);
	expression2 = (rule.size () > 3) ? children.get (3) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("assert ").append (expression1);
	if (expression2 != null)
	    sb.append (" : ").append (expression2);
	return sb.toString ();
    }
}
