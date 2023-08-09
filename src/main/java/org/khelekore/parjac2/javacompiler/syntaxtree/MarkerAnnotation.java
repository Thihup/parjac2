package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;
import java.util.Objects;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MarkerAnnotation extends Annotation {
    private TypeName typename;
    public MarkerAnnotation (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	this.typename = (TypeName)children.get (1);
    }

    public MarkerAnnotation (ParsePosition pos, TypeName typename) {
	super (pos);
	this.typename = typename;
    }

    @Override public Object getValue() {
	return "@" + typename;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (typename);
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	MarkerAnnotation ma = (MarkerAnnotation)o;
	return Objects.equals (typename, ma.typename);
    }
}
