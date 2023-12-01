package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableInitializerList extends SyntaxTreeNode {
    private final List<ParseTreeNode> variableInitializers;

    public VariableInitializerList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	variableInitializers = new ArrayList<> ();
	variableInitializers.add (children.get (0));
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int j = 1, e = z.size (); j < e; j += 2)
		variableInitializers.add (z.get (j));
	}
    }

    public int size () {
	return variableInitializers.size ();
    }

    public List<ParseTreeNode> variableInitializers () {
	return variableInitializers;
    }

    @Override public Object getValue () {
	return variableInitializers;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	variableInitializers.forEach (v::accept);
    }
}
