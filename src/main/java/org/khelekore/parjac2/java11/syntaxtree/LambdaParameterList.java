package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LambdaParameterList<T> extends SyntaxTreeNode {
    private final List<T> params;
    public LambdaParameterList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children,
				Function<ParseTreeNode, T> nodeConverter) {
	super (n.getPosition ());
	if (rule.size () == 1) {
	    params = List.of (nodeConverter.apply (children.get (0)));
	} else {
	    params = new ArrayList<> ();
	    params.add (nodeConverter.apply (children.get (0)));
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1; i < z.size (); i += 2)
		params.add (nodeConverter.apply (z.get (i)));
	}
    }

    @Override public Object getValue () {
	return params;
    }
}
