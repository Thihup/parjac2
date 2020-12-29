package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeParameters extends SyntaxTreeNode {
    private final TypeParameterList list;

    public TypeParameters (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	this.list = (TypeParameterList)children.get (1);
    }

    @Override public Object getValue () {
	return "<" + list + ">";
    }
}
