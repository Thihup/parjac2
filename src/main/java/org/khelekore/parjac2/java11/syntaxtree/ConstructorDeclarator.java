package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorDeclarator extends SyntaxTreeNode {
    private TypeParameters types;
    private String id;
    private ReceiverParameter rp;
    private FormalParameterList params;
    public ConstructorDeclarator (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("TypeParameters"))
	    types = (TypeParameters)children.get (i++);
	id = ((Identifier)children.get (i++)).getValue ();
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("ReceiverParameter")) {
	    rp = (ReceiverParameter)children.get (i);
	    i += 2;
	}
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("FormalParameterList"))
	    params = (FormalParameterList)children.get (i++);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (types != null)
	    sb.append (types).append (" ");
	sb.append (id).append (" (");
	if (rp != null) {
	    sb.append (rp);
	    if (params != null)
		sb.append (", ");
	}
	if (params != null)
	    sb.append (params);
	sb.append (")");
	return sb.toString ();
    }
}
