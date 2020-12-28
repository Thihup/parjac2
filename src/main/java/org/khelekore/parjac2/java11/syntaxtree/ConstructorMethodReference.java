package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorMethodReference extends SyntaxTreeNode {
    private final ParseTreeNode type;
    private final TypeArguments types;
    public ConstructorMethodReference (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	type = children.get (0);
	types = r.size () > 3 ? (TypeArguments)children.get (2) : null;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (type).append ("::");
	if (types != null)
	    sb.append (types);
	sb.append ("new");
	return sb.toString ();
    }
}
