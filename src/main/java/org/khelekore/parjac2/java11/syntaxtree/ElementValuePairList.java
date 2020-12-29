package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ElementValuePairList extends SyntaxTreeNode {
    private final List<ElementValuePair> values;

    public ElementValuePairList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () == 1) {
	    values = List.of ((ElementValuePair)children.get (0));
	} else {
	    values = new ArrayList<> ();
	    values.add ((ElementValuePair)children.get (0));
	    Multiple z = (Multiple)children.get (1);
	    for (int j = 1, e = z.size (); j < e; j += 2)
		values.add ((ElementValuePair)z.get (j));
	}
    }

    @Override public Object getValue() {
	return values;
    }
}
