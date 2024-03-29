package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FormalParameterList extends SyntaxTreeNode {
    private List<FormalParameterBase> params;

    public FormalParameterList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	params = new ArrayList<> ();
	params.add ((FormalParameterBase)children.get (0));
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1, e = z.size (); i < e; i += 2)
		params.add ((FormalParameterBase)z.get (i));
	}
    }

    public FormalParameterList (ParsePosition pos, List<FormalParameterBase> params) {
	super (pos);
	this.params = params;
    }

    @Override public Object getValue () {
	return params;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	params.forEach (v::accept);
    }

    public List<FormalParameterBase> getParameters () {
	return params;
    }

    public int size () {
	return params.size ();
    }

    public boolean isVarArgs () {
	return params != null && params.size () > 0 && params.get (params.size () - 1) instanceof VariableArityParameter;
    }
}
