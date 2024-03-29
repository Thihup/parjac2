package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeParameters extends SyntaxTreeNode {
    private final TypeParameterList list;

    public TypeParameters (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	this.list = (TypeParameterList)children.get (1);
    }

    @Override public Object getValue () {
	return "<" + list + ">";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (list);
    }

    public Iterable<TypeParameter> get () {
	List<ParseTreeNode> ls = list.get ();
	return ls.stream ().map (p -> ((TypeParameter)p)).toList ();
    }
}
