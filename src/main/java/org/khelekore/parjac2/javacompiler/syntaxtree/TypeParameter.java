package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeParameter extends SyntaxTreeNode {
    private List<Annotation> annotations;
    private String id;
    private TypeBound bound;

    public TypeParameter (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple) {
	    annotations = ((Multiple)children.get (i++)).get ();
	} else {
	    annotations = Collections.emptyList ();
	}
	id = ((Identifier)children.get (i++)).getValue ();
	bound = rule.size () > i ? (TypeBound)children.get (i) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append (id);
	if (bound != null)
	    sb.append (" ").append (bound);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (v::accept);
	if (bound != null)
	    v.accept (bound);
    }

    public String getId () {
	return id;
    }

    public TypeBound getTypeBound () {
	return bound;
    }

    public ExpressionType getExpressionType () {
	if (bound != null)
	    return bound.getExpressionType ();
	return ExpressionType.OBJECT;
    }
}
