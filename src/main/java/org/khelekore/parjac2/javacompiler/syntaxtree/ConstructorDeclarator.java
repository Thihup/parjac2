package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorDeclarator extends SyntaxTreeNode {
    private final TypeParameters types;
    private final String id;
    private final ReceiverParameter rp;
    private final FormalParameterList params;

    public ConstructorDeclarator (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("TypeParameters"))
	    types = (TypeParameters)children.get (i++);
	else
	    types = null;
	id = ((Identifier)children.get (i++)).getValue ();
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("ReceiverParameter")) {
	    rp = (ReceiverParameter)children.get (i);
	    i += 2;
	} else {
	    rp = null;
	}
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("FormalParameterList"))
	    params = (FormalParameterList)children.get (i++);
	else
	    params = null;
    }

    public ConstructorDeclarator (ParsePosition pos, String id) {
	super (pos);
	this.types = null;
	this.id = id;
	this.rp = null;
	this.params = null;
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

    @Override public void visitChildNodes (NodeVisitor v) {
	if (types != null)
	    v.accept (types);
	if (rp != null)
	    v.accept (rp);
	if (params != null)
	    v.accept (params);
    }

    public TypeParameters getTypeParameters () {
	return types;
    }

    public String getName () {
	return id;
    }

    public ReceiverParameter getReceiverParameter () {
	return rp;
    }

    public FormalParameterList getFormalParameterList () {
	return params;
    }
}
