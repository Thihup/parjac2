package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ElementValuePair extends SyntaxTreeNode {
    private final String id;
    private final ParseTreeNode value;

    public ElementValuePair (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	id = ((Identifier)children.get (0)).getValue ();
	value = children.get (2);
    }

    @Override public Object getValue() {
	return id + " = " + value;
    }
}
