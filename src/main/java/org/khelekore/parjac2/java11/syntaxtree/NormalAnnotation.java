package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class NormalAnnotation extends Annotation {
    private TypeName typename;
    private ParseTreeNode elementValuePairList;
    public NormalAnnotation (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	this.typename = (TypeName)children.get (1);
	elementValuePairList = rule.size () > 4 ? children.get (3) : null;
    }

    @Override public Object getValue() {
	return "@" + typename + "(" + (elementValuePairList != null ? elementValuePairList : "") + ")";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (typename);
	if (elementValuePairList != null)
	    v.accept (elementValuePairList);
    }
}