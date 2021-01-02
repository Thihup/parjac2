package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumDeclaration extends TypeDeclaration {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final Superinterfaces supers;
    private final EnumBody body;

    public EnumDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
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

    @Override public String getName () {
	return id;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }
}
