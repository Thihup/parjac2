package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.FieldInfo;
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
    private ClassPermits classPermits;
    private ClassBody body;

    private static FlagCalculator flagCalculator = FlagCalculator.SIMPLE_ACCESS;

    public NormalClassDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	// {ClassModifier} class TypeIdentifier [TypeParameters] [ClassExtends] [ClassImplements] [ClassPermits] ClassBody
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    modifiers = ((Multiple)children.get (i++)).get ();
	else
	    modifiers = Collections.emptyList ();
	i++; // 'class'
	id = ((TypeIdentifier)children.get (i++)).getValue ();
	if (children.get (i) instanceof TypeParameters tp) {
	    typeParameters = tp;
	    i++;
	}
	if (children.get (i) instanceof Superclass sc) {
	    superClass = sc;
	    i++;
	}
	if (children.get (i) instanceof Superinterfaces si) {
	    superInterfaces = si;
	    i++;
	}
	if (children.get (i) instanceof ClassPermits cp) {
	    classPermits = cp;
	    i++;
	}
	body = (ClassBody)children.get (i++);
	flags = flagCalculator.calculate (ctx, modifiers, position ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append ("class ").append (id).append (" ");
	if (typeParameters != null)
	    sb.append (typeParameters).append (" ");
	if (superClass != null)
	    sb.append (superClass).append (" ");
	if (superInterfaces != null)
	    sb.append (superInterfaces).append (" ");
	if (classPermits != null)
	    sb.append (classPermits).append (" ");
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

    @Override public TypeParameters getTypeParameters () {
	return typeParameters;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }

    @Override public boolean isLocalClass (TypeDeclaration td) {
	return body.isLocalClass (td);
    }

    @Override public ClassType getSuperClass () {
	return (superClass != null) ? superClass.getType () : null;
    }

    public List<ClassType> getSuperInterfaces () {
	return (superInterfaces != null) ? superInterfaces.getTypes () : null;
    }

    @Override public Map<String, FieldInfo> getFields () {
	return body.getFields ();
    }

    @Override public List<MethodDeclaration> getMethods () {
	return body.getMethods ();
    }

    @Override public List<ConstructorDeclaration> getConstructors () {
	return body.getConsructors ();
    }

    public void addConstructor (ConstructorDeclaration cd) {
	getConstructors ().add (cd);
	clearMethodInfoCache ();
    }

    @Override public List<SyntaxTreeNode> getInstanceInitializers () {
	return body.getInstanceInitializers ();
    }

    @Override public List<SyntaxTreeNode> getStaticInitializers () {
	return body.getStaticInitializers ();
    }
}
