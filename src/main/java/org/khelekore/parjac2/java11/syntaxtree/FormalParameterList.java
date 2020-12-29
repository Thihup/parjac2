package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FormalParameterList extends SyntaxTreeNode {
    List<FormalParameterBase> params;
    public FormalParameterList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	params = new ArrayList<> ();
	params.add ((FormalParameterBase)children.get (0));
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1, e = z.size (); i < e; i += 2)
		params.add ((FormalParameterBase)z.get (i));
	}
    }

    @Override public Object getValue() {
	return params;
    }
}
