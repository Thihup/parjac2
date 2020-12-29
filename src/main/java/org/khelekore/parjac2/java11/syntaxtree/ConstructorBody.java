package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorBody extends SyntaxTreeNode {
    private ExplicitConstructorInvocation eci;
    private BlockStatements statements;
    public ConstructorBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 1;
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("ExplicitConstructorInvocation"))
	    eci = (ExplicitConstructorInvocation)children.get (i++);
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("BlockStatements"))
	    statements = (BlockStatements)children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{\n");
	if (eci != null)
	    sb.append (eci);
	if (statements != null)
	    sb.append (statements);
	sb.append ("\n}");
	return sb.toString ();
    }
}
