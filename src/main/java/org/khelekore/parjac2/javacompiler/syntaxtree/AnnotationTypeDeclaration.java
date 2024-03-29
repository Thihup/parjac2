package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.FieldInfo;
import org.khelekore.parjac2.javacompiler.TypeIdentifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class AnnotationTypeDeclaration extends TypeDeclaration {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final AnnotationTypeBody body;

    private static FlagCalculator flagCalculator = FlagCalculator.SIMPLE_ACCESS;

    public AnnotationTypeDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    modifiers = ((Multiple)children.get (i++)).get ();
	else
	    modifiers = List.of ();
	i += 2;
	id = ((TypeIdentifier)children.get (i++)).getValue ();
	body = (AnnotationTypeBody)children.get (i);
	flags = flagCalculator.calculate (ctx, modifiers, position ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append ("@interface ").append (id).append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (body);
    }

    @Override public String getName () {
	return id;
    }

    @Override public TypeParameters getTypeParameters () {
	return null;
    }

    @Override public ClassType getSuperClass () {
	return null;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }

    @Override public boolean isLocalClass (TypeDeclaration td) {
	return body.isLocalClass (td);
    }

    @Override public Map<String, FieldInfo> getFields () {
	return body.getFields ();
    }

    @Override public List<MethodDeclaration> getMethods () {
	return List.of ();
    }

    @Override public List<ConstructorDeclaration> getConstructors () {
	return List.of ();
    }

    @Override public List<SyntaxTreeNode> getInstanceInitializers () {
	return List.of ();
    }

    @Override public List<SyntaxTreeNode> getStaticInitializers () {
	return List.of ();
    }
}
