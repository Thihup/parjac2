package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.FieldInfo;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumDeclaration extends TypeDeclaration {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final Superinterfaces supers;
    private final EnumBody body;

    private static FlagCalculator flagCalculator = FlagCalculator.SIMPLE_ACCESS;
    private final static ClassType ENUM_SUPER = new ClassType (FullNameHandler.JL_ENUM);

    public EnumDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	if (children.get (0) instanceof Multiple) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	i++;
	id = ((Identifier)children.get (i++)).getValue ();
	if (rule.size () > i + 1) {
	    supers = (Superinterfaces)children.get (i++);
	} else {
	    supers = null;
	}
	body = (EnumBody)children.get (i);
	body.setParents (this);
	flags = flagCalculator.calculate (ctx, modifiers, position ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append ("enum ").append (id).append (" ");
	if (supers != null)
	    sb.append (supers).append (" ");
	sb.append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	if (supers != null)
	    v.accept (supers);
	v.accept (body);
    }

    @Override public String getName () {
	return id;
    }

    @Override public TypeParameters getTypeParameters () {
	return null;
    }

    @Override public ClassType getSuperClass () {
	return ENUM_SUPER;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }

    @Override public boolean isLocalClass (TypeDeclaration td) {
	return body.isLocalClass (td);
    }

    public List<ClassType> getSuperInterfaces () {
	return (supers != null) ? supers.getTypes () : null;
    }

    @Override public Map<String, FieldInfo> getFields () {
	return body.getFields ();
    }

    @Override public List<MethodDeclaration> getMethods () {
	return body.getMethods ();
    }

    public void addMethod (MethodDeclaration m) {
	getMethods ().add (m);
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

    public List<EnumConstant> constants () {
	return body.constants ();
    }
}
