package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class NormalInterfaceDeclaration extends TypeDeclaration {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final TypeParameters types;
    private final ExtendsInterfaces extendsInterfaces;
    private final InterfaceBody body;

    public NormalInterfaceDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	i++;
	id = ((Identifier)children.get (i++)).getValue ();
	if (children.get (i) instanceof TypeParameters) {
	    types = (TypeParameters)children.get (i++);
	} else {
	    types = null;
	}
	if (children.get (i) instanceof ExtendsInterfaces) {
	    extendsInterfaces = (ExtendsInterfaces)children.get (i++);
	} else {
	    extendsInterfaces = null;
	}
	body = (InterfaceBody)children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append ("interface ").append (id);
	if (types != null)
	    sb.append (" ").append (types);
	if (extendsInterfaces != null)
	    sb.append (" ").append (extendsInterfaces);
	sb.append (body);
	return sb.toString ();
    }

    @Override public String getName () {
	return id;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	if (types != null)
	    v.accept (types);
	if (extendsInterfaces != null)
	    v.accept (extendsInterfaces);
	v.accept (body);
    }

    @Override public int getFlags () {
	// TODO: fill in from modifiers
	return 0;
    }
}
