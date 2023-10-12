package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodDeclarator extends SyntaxTreeNode {
    private String id;
    private ReceiverParameter rp;
    private FormalParameterList params;
    private Dims dims;

    // Identifier ( [ReceiverParameter ,] [FormalParameterList] ) [Dims] 
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

    @Override public void visitChildNodes (NodeVisitor v) {
	if (rp != null)
	    v.accept (rp);
	if (params != null)
	    v.accept (params);
	if (dims != null)
	    v.accept (dims);
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

    public Dims getDims () {
	return dims;
    }
}
