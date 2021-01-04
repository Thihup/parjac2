package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;

public class AnnotatedIdentifier extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final String id;

    public AnnotatedIdentifier (ParsePosition pos, List<Annotation> annotations, Identifier id) {
	super (pos);
	this.annotations = annotations;
	this.id = id.getValue ();
    }

    public List<Annotation> getAnnotations () {
	return annotations;
    }

    public String getIdentifier () {
	return id;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	annotations.forEach (a -> sb.append (a).append (" "));
	sb.append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (v::accept);
    }
}
