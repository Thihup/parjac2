package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Wildcard extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final WildcardBounds bounds;

    public Wildcard (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (0) instanceof Multiple)
	    annotations = ((Multiple)children.get (i++)).get ();
	else
	    annotations = Collections.emptyList ();
	bounds = (rule.size () > i + 1) ? (WildcardBounds)children.get (i + 1) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append ("?");
	if (bounds != null)
	    sb.append (" ").append (bounds);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (v::accept);
	v.accept (bounds);
    }
}
