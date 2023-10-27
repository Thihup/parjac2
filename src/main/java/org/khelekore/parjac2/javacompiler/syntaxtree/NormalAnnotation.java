package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class NormalAnnotation extends Annotation {
    private ParseTreeNode elementValuePairList;
    public NormalAnnotation (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position (), children);
	elementValuePairList = rule.size () > 4 ? children.get (3) : null;
    }

    @Override public Object getValue() {
	return "@" + getTypeName () + "(" + (elementValuePairList != null ? elementValuePairList : "") + ")";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (getTypeName ());
	if (elementValuePairList != null)
	    v.accept (elementValuePairList);
    }
}
