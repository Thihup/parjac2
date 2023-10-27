package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ElementValueList extends SyntaxTreeNode {
    private final List<ParseTreeNode> values;

    public ElementValueList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	if (rule.size () == 1) {
	    values = List.of (children.get (0));
	} else {
	    values = new ArrayList<> ();
	    values.add (children.get (0));
	    Multiple z = (Multiple)children.get (1);
	    for (int j = 1, e = z.size (); j < e; j += 2)
		values.add (z.get (j));
	}
    }
    @Override public Object getValue () {
	return values;
    }

    public List<ParseTreeNode> getValues () {
	return values;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	values.forEach (v::accept);
    }
}
