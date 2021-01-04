package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeBound extends SyntaxTreeNode {
    private ClassType base;
    private List<ClassType> additionalBounds;

    public TypeBound (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	base = (ClassType)children.get (1);
	if (rule.size () > 2) {
	    additionalBounds = new ArrayList<> ();
	    Multiple z = (Multiple)children.get (2);
	    z.forEach (c -> additionalBounds.add ((ClassType)c));
	} else {
	    additionalBounds = Collections.emptyList ();
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("extends ").append (base);
	for (ClassType c : additionalBounds)
	    sb.append (" & ").append (c);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (base);
	additionalBounds.forEach (v::accept);
    }
}
