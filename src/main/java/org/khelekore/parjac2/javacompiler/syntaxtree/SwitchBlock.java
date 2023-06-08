package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchBlock extends SyntaxTreeNode {
    private List<SwitchBlockStatementGroup> ls;
    private List<SwitchLabel> trailingLabels;

    public SwitchBlock (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	ls = new ArrayList<> ();
	trailingLabels = new ArrayList<> ();
	int i = 1;
	if (children.get (i) instanceof Multiple) {
	    Multiple z = (Multiple)children.get (i++);
	    if (z.getInternalGroupId () == ctx.getGrammar ().getRuleGroupId ("SwitchBlockStatementGroup")) {
		ls = z.get ();
	    } else {
		trailingLabels = z.get ();
	    }
	}
	if (children.get (i) instanceof Multiple) {
	    Multiple z = (Multiple)children.get (i);
	    trailingLabels = z.get ();
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	for (SwitchBlockStatementGroup g : ls)
	    sb.append (g.toString ());
	for (SwitchLabel l : trailingLabels)
	    sb.append (l.toString ());
	sb.append ("}");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	ls.forEach (v::accept);
	trailingLabels.forEach (v::accept);
    }
}
