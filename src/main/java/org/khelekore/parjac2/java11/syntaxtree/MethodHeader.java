package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodHeader extends SyntaxTreeNode {
    private TypeParameters types;
    private List<ParseTreeNode> annotations;
    private ParseTreeNode result;
    private MethodDeclarator methodDeclarator;
    private Throws t;

    public MethodHeader (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	annotations = Collections.emptyList ();
	if (rule.get (0) == ctx.getGrammar ().getRuleGroupId ("TypeParameters")) {
	    types = (TypeParameters)children.get (i++);
	    if (children.get (i) instanceof Multiple)
		annotations = ((Multiple)children.get (i++)).get ();
	}
	result = children.get (i++);
	methodDeclarator = (MethodDeclarator)children.get (i++);
	if (rule.size () > i)
	    t = (Throws)children.get (i);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (types != null)
	    sb.append (types).append (" ");
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append (result).append (" ").append (methodDeclarator);
	if (t != null)
	    sb.append (" ").append (t);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (types != null)
	    v.accept (types);
	annotations.forEach (v::accept);
	v.accept (result);
	v.accept (methodDeclarator);
	if (t != null)
	    v.accept (t);
    }
}
