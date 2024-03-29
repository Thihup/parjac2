package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodHeader extends SyntaxTreeNode {
    private final TypeParameters types;
    private final List<ParseTreeNode> annotations;
    private final ParseTreeNode result;
    private final MethodDeclarator methodDeclarator;
    private final Throws t;
    private String signature;

    // Result MethodDeclarator [Throws]
    // TypeParameters {Annotation} Result MethodDeclarator [Throws]
    public MethodHeader (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	List<ParseTreeNode> anns = List.of ();
	if (rule.get (0) == ctx.getGrammar ().getRuleGroupId ("TypeParameters")) {
	    types = (TypeParameters)children.get (i++);
	    if (children.get (i) instanceof Multiple)
		anns = ((Multiple)children.get (i++)).get ();
	} else {
	    types = null;
	}
	this.annotations = anns;
	result = children.get (i++);
	methodDeclarator = (MethodDeclarator)children.get (i++);
	t = rule.size () > i ? (Throws)children.get (i) : null;
    }

    /** Create a method header for a method with a given name and return type, no arguments */
    public MethodHeader (ParsePosition pos, String name, ParseTreeNode result, FormalParameterList params) {
	super (pos);
	types = null;
	annotations = List.of ();
	this.result = result;
	methodDeclarator = new MethodDeclarator (pos, name, params);
	t = null;
    }

    @Override public Object getValue () {
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

    public TypeParameters getTypeParameters () {
	return types;
    }

    public List<ParseTreeNode> getAnnotations () {
	return annotations;
    }

    public ParseTreeNode getResult () {
	return result;
    }

    public String getName () {
	return methodDeclarator.getName ();
    }

    public ReceiverParameter getReceiverParameter () {
	return methodDeclarator.getReceiverParameter ();
    }

    public FormalParameterList getFormalParameterList () {
	return methodDeclarator.getFormalParameterList ();
    }

    public String signature () {
	if (types == null)
	    return null;
	return createAndCacheSignature ();
    }

    private synchronized String createAndCacheSignature () {
	if (signature != null)
	    return signature;

	return "";
    }

    public Dims getDims () {
	return methodDeclarator.getDims ();
    }

    public Throws getThrows () {
	return t;
    }

    public List<ClassType> thrownTypes () {
	return t == null ? null : t.thrownTypes ();
    }

    public boolean isVarArgs () {
	return methodDeclarator.isVarArgs ();
    }
}
