package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.JavaTokens;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorDeclaration extends ConstructorBase {
    private final List<ParseTreeNode> modifiers;
    private final ConstructorDeclarator declarator;
    private final Throws t;
    private final ConstructorBody body;

    private static FlagCalculator flagCalculator = FlagCalculator.SIMPLE_ACCESS;

    public ConstructorDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    modifiers = ((Multiple)children.get (i++)).get ();
	else
	    modifiers = Collections.emptyList ();
	declarator = (ConstructorDeclarator)children.get (i++);
	t = (rule.size () > i + 1) ? (Throws)children.get (i++) : null;
	body = (ConstructorBody)children.get (i);
	flags = flagCalculator.calculate (ctx, modifiers, position ());
    }

    public static ConstructorDeclaration create (ParsePosition pos, JavaTokens javaTokens,
						 int flags, String id, List<ParseTreeNode> superCallArguments,
						 List<FormalParameterBase> parameters, List<ParseTreeNode> bodyContent) {
	ConstructorDeclarator d = new ConstructorDeclarator (pos, id, parameters);
	ArgumentList args = new ArgumentList (pos, superCallArguments);
	ExplicitConstructorInvocation eci = new ExplicitConstructorInvocation (pos, javaTokens, args);
	ConstructorBody body = new ConstructorBody (pos, eci, bodyContent);
	return new ConstructorDeclaration (pos, flags, d, body);
    }

    private ConstructorDeclaration (ParsePosition pos, int flags, ConstructorDeclarator d, ConstructorBody body) {
	super (pos);
	this.modifiers = List.of ();
	this.declarator = d;
	this.t = null;
	this.body = body;
	this.flags = flags;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (declarator).append (" ");
	if (t != null)
	    sb.append (t).append (" ");
	sb.append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (declarator);
	if (t != null)
	    v.accept (t);
	v.accept (body);
    }

    @Override public List<ParseTreeNode> getAnnotations () {
	return Annotation.getAnnotations (modifiers);
    }

    @Override public TypeParameters getTypeParameters () {
	return declarator.getTypeParameters ();
    }

    @Override public String getName () {
	return declarator.getName ();
    }

    @Override public ReceiverParameter getReceiverParameter () {
	return declarator.getReceiverParameter ();
    }

    @Override public FormalParameterList getFormalParameterList () {
	return declarator.getFormalParameterList ();
    }

    @Override public List<ParseTreeNode> statements () {
	return body.statements ();
    }

    @Override public ConstructorBody body () {
	return body;
    }
}
