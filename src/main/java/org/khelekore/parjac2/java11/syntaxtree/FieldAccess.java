package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FieldAccess extends SyntaxTreeNode {
    private final ParseTreeNode from;
    private final boolean isSuper;
    private final String id;

    public FieldAccess (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.get (0) != ctx.getTokens ().SUPER.getId ()) {
	    from = children.get (0);
	    isSuper = rule.size () > 3;
	} else {
	    from = null;
	    isSuper = true;
	}
	id = ((Identifier)children.get (children.size () - 1)).getValue ();
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (from != null)
	    sb.append (from).append (".");
	if (isSuper)
	    sb.append ("super");
	sb.append (".").append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (from);
    }
}
