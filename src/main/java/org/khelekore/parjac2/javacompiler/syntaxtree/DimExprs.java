package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DimExprs extends SyntaxTreeNode {
    private final List<DimExpr> dims;

    public DimExprs (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	if (rule.size () == 1) {
	    dims = List.of ((DimExpr)children.get (0));
	} else {
	    dims = new ArrayList<> ();
	    dims.add ((DimExpr)children.get (0));
	    Multiple z = (Multiple)children.get (1);
	    dims.addAll (z.get ());
	}
    }

    public DimExprs (ParsePosition pos, ParseTreeNode expression) {
	super (pos);
	dims = List.of (new DimExpr (pos, expression));
    }

    @Override public Object getValue () {
	return dims;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	dims.forEach (v::accept);
    }

    public int rank () {
	return dims.size ();
    }
}
