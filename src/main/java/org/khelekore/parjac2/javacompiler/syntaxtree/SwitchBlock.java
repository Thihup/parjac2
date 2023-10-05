package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class SwitchBlock extends SyntaxTreeNode {

    private SwitchBlock (ParsePosition p) {
	super (p);
    }

    public static SwitchBlock build (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	/*
	  SwitchBlock:
                  '{' SwitchRule {SwitchRule} '}'
		  '{' {SwitchBlockStatementGroup} {SwitchLabel} '}'
	*/
	if (children.get (1) instanceof SwitchRule)
	    return new SwitchBlockRule (rule, n, children);
	return new SwitchBlockStatements (ctx, rule, n, children);
    }

    public static class SwitchBlockStatements extends SwitchBlock {
	private final List<SwitchBlockStatementGroup> ls;
	private final List<SwitchLabel> trailingLabels;

	public SwitchBlockStatements (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());

	    ls = new ArrayList<> ();
	    trailingLabels = new ArrayList<> ();
	    int i = 1;
	    if (children.get (i) instanceof Multiple) {
		Multiple z = (Multiple)children.get (i++);
		if (z.getInternalGroupId () == ctx.getGrammar ().getRuleGroupId ("SwitchBlockStatementGroup")) {
		    ls.addAll (z.get ());
		} else {
		    trailingLabels.addAll (z.get ());
		}
	    }
	    if (children.get (i) instanceof Multiple z) {
		trailingLabels.addAll (z.get ());
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

    public static class SwitchBlockRule extends SwitchBlock {
	private List<SwitchRule> rules = new ArrayList<> ();

	public SwitchBlockRule (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    rules.add ((SwitchRule)children.get (1));
	    if (rule.size () > 3)
		rules.addAll (((Multiple)children.get (2)).get ());
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("{");
	    rules.forEach (r -> sb.append (r.getValue ()));
	    sb.append ("}");
	    return sb.toString ();
	}

	@Override public void visitChildNodes (NodeVisitor v) {
	    rules.forEach (v::accept);
	}
    }
}
