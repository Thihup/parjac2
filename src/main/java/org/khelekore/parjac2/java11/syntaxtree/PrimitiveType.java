package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class PrimitiveType extends SyntaxTreeNode {
    private List<Annotation> annotations;
    private Token type;

    public PrimitiveType (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 1) {
	    annotations = ((Multiple)children.get (0)).get ();
	    type = ((TokenNode)children.get (1)).getToken ();
	} else {
	    annotations = Collections.emptyList ();
	    type = ((TokenNode)children.get (0)).getToken ();
	}
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append (type);
	return sb.toString ();
    }
}
