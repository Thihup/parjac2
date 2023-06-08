package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class InterfaceTypeList extends SyntaxTreeNode {
    private List<ClassType> types;
    public InterfaceTypeList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	types = new ArrayList<> ();
	types.add ((ClassType)children.get (0));
	if (children.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1, e = z.size (); i < e; i += 2)
		types.add ((ClassType)z.get (i));
	}
    }

    @Override public Object getValue () {
	return types;
    }

    public List<ClassType> getTypes () {
	return types;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	types.forEach (v::accept);
    }
}
