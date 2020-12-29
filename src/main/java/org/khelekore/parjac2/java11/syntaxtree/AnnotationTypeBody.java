package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class AnnotationTypeBody extends SyntaxTreeNode {
    private final List<ParseTreeNode> members;

    public AnnotationTypeBody (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 2) {
	    members = ((Multiple)children.get (1)).get ();
	} else {
	    members = List.of ();
	}
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	if (!members.isEmpty ())
	    sb.append (members);
	sb.append ("}");
	return sb.toString ();
    }
}
