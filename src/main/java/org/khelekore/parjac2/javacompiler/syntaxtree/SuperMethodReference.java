package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SuperMethodReference extends MethodReference {
    private final ParseTreeNode type;
    private final TypeArguments types;
    private final String id;

    public SuperMethodReference (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	/*
	'super' '::' [TypeArguments] 'Identifier'
	TypeName '.' 'super' '::' [TypeArguments] 'Identifier'

	if TypeName is given it is supposed to be an enclosing type
	*/
	if (r.size () > 4) {
	    type = children.get (0);
	    types = r.size () > 5 ? (TypeArguments)children.get (4) : null;
	} else {
	    type = null;
	    types = r.size () > 3 ? (TypeArguments)children.get (2) : null;
	}
	id = ((Identifier)children.get (children.size () - 1)).getValue ();
    }

    public ParseTreeNode onType () {
	return type;
    }

    @Override public String name () {
	return id;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (type != null)
	    sb.append (type).append (".");
	sb.append ("super::");
	if (types != null)
	    sb.append (types);
	sb.append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (type != null)
	    v.accept (type);
	if (types != null)
	    v.accept (types);
    }
}
