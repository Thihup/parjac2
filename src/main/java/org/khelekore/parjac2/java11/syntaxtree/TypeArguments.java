package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeArguments extends SyntaxTreeNode {
    private final List<ParseTreeNode> typeArgumentList;

    public TypeArguments (Rule rule, ParseTreeNode sct, List<ParseTreeNode> children) {
	super (sct.getPosition ());
	// '<' TypeArgumentList '>'
	typeArgumentList = ((TypeArgumentList)children.get (1)).get ();
    }

    @Override public Object getValue () {
	return "<" + typeArgumentList + ">";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	typeArgumentList.forEach (v::accept);
    }
}