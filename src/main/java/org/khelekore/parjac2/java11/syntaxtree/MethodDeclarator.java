package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodDeclarator extends SyntaxTreeNode {
    private String id;
    private ReceiverParameter rp;
    private FormalParameterList params;
    private Dims dims;

    public MethodDeclarator (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	id = ((Identifier)children.get (0)).getValue ();
	int i = 2;
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("ReceiverParameter")) {
	    rp = (ReceiverParameter)children.get (i);
	    i += 2;
	}
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("FormalParameterList"))
	    params = (FormalParameterList)children.get (i++);
	i++;
	if (rule.size () > i)
	    dims = (Dims)children.get (i);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (id).append ("(");
	if (rp != null)
	    sb.append (rp).append (", ");
	if (params != null)
	    sb.append (params);
	sb.append (")");
	if (dims != null)
	    sb.append (dims);
	return sb.toString ();
    }
}
