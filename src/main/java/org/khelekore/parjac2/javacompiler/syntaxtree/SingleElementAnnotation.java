package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SingleElementAnnotation extends Annotation {
    private ParseTreeNode value;

    public SingleElementAnnotation (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position (), children);
	value = children.get (3);
    }

    @Override public Object getValue() {
	return "@" + getTypeName () + "(" + value + ")";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (getTypeName ());
	v.accept (value);
    }
}
