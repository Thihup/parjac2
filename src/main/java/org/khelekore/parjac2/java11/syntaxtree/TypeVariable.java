package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeVariable extends SyntaxTreeNode {
    private List<Annotation> annotations;
    private String id;

    public TypeVariable (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 1) {
	    annotations = ((Multiple)children.get (0)).get ();
	    id = ((Identifier)children.get (1)).getValue ();
	} else {
	    annotations = Collections.emptyList ();
	    id = ((Identifier)children.get (0)).getValue ();
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (v::accept);
    }
}
