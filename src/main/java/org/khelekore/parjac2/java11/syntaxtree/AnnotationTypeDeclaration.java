package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.TypeIdentifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class AnnotationTypeDeclaration extends TypeDeclaration {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final AnnotationTypeBody body;

    public AnnotationTypeDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    modifiers = ((Multiple)children.get (i++)).get ();
	else
	    modifiers = List.of ();
	i += 2;
	id = ((TypeIdentifier)children.get (i++)).getValue ();
	body = (AnnotationTypeBody)children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append ("@interface ").append (id).append (body);
	return sb.toString ();
    }

    @Override public String getName () {
	return id;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }
}
