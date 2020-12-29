package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CastExpression extends SyntaxTreeNode {
    private ParseTreeNode baseType;
    private List<ClassType> additionalBounds;
    private ParseTreeNode expression;

    public CastExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	// starts with (
	int i = 1;
	baseType = children.get (i++);
	if (children.get (i) instanceof Multiple) {
	    additionalBounds = new ArrayList<> ();
	    Multiple z = (Multiple)children.get (i++);
	    z.forEach (c -> additionalBounds.add ((ClassType)c));
	} else {
	    additionalBounds = Collections.emptyList ();
	}
	i++; // ')'
	expression = children.get (i);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("(").append (baseType);
	if (!additionalBounds.isEmpty ())
	    additionalBounds.forEach (a -> sb.append (" & ").append (a));
	sb.append (")").append (expression);
	return sb.toString ();
    }
}
