package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SuperMethodReference extends SyntaxTreeNode {
    private final ParseTreeNode type;
    private final TypeArguments types;
    private final String id;

    public SuperMethodReference (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	type = children.get (0);
	types = r.size () > 5 ? (TypeArguments)children.get (4) : null;
	id = ((Identifier)children.get (children.size () - 1)).getValue ();
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (type).append (".super::");
	if (types != null)
	    sb.append (types);
	sb.append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (type);
	v.accept (types);
    }
}
