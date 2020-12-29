package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DimExprs extends SyntaxTreeNode {
    private final List<DimExpr> dims;

    public DimExprs (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () == 1) {
	    dims = List.of ((DimExpr)children.get (0));
	} else {
	    dims = new ArrayList<> ();
	    dims.add ((DimExpr)children.get (0));
	    Multiple z = (Multiple)children.get (1);
	    dims.addAll (z.get ());
	}
    }

    @Override public Object getValue () {
	return dims;
    }
}
