package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;
import java.util.Objects;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class MarkerAnnotation extends Annotation {
    public MarkerAnnotation (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position (), children);
    }

    public MarkerAnnotation (ParsePosition pos, TokenNode at, TypeName typename) {
	super (pos, List.of (at, typename));
    }

    @Override public Object getValue() {
	return "@" + getTypeName ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (getTypeName ());
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	MarkerAnnotation ma = (MarkerAnnotation)o;
	return Objects.equals (getTypeName (), ma.getTypeName ());
    }
}
