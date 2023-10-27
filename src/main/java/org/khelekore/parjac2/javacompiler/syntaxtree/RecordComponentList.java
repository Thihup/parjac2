package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class RecordComponentList extends SyntaxTreeNode {
    private final List<RecordComponent> components = new ArrayList<> ();

    public RecordComponentList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	components.add ((RecordComponent)children.get (0));
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1, e = z.size (); i < e; i += 2)
		components.add ((RecordComponent)z.get (i));
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (components.get (0));
	for (int i = 1; i < components.size (); i++)
	    sb.append (", ") .append (components.get (i).getValue ());
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	components.forEach (v::accept);
    }

    public List<RecordComponent> get () {
	return components;
    }
}
