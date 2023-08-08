package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.TypeIdentifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class NormalClassDeclaration extends TypeDeclaration {
    private List<ParseTreeNode> modifiers;
    private String id;
    private TypeParameters typeParameters;
    private Superclass superClass;
    private Superinterfaces superInterfaces;
    private ClassBody body;

    private static FlagCalculator flagCalculator = FlagCalculator.SIMPLE_ACCESS;

    public NormalClassDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	// {ClassModifier} class TypeIdentifier [TypeParameters] [ClassExtends] [ClassImplements] [ClassPermits] ClassBody
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    modifiers = ((Multiple)children.get (i++)).get ();
	else
	    modifiers = Collections.emptyList ();
	i++; // 'class'
	id = ((TypeIdentifier)children.get (i++)).getValue ();
	if (children.get (i) instanceof TypeParameters)
	    typeParameters = (TypeParameters)children.get (i++);
	if (children.get (i) instanceof Superclass)
	    superClass = (Superclass)children.get (i++);
	if (children.get (i) instanceof Superinterfaces)
	    superInterfaces = (Superinterfaces)children.get (i++);
	body = (ClassBody)children.get (i++);
	flags = flagCalculator.calculate (ctx, modifiers, getPosition ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append ("class ").append (id);
	if (typeParameters != null)
	    sb.append (typeParameters).append (" ");
	if (superClass != null)
	    sb.append (superClass).append (" ");
	if (superInterfaces != null)
	    sb.append (superInterfaces).append (" ");
	sb.append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	if (typeParameters != null)
	    v.accept (typeParameters);
	if (superClass != null)
	    v.accept (superClass);
	if (superInterfaces != null)
	    v.accept (superInterfaces);
	v.accept (body);
    }

    @Override public String getName () {
	return id;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }
}
