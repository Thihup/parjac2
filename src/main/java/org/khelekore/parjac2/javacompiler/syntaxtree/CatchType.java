package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CatchType extends SyntaxTreeNode {
    private final UnannClassType firstType;
    private final List<ClassType> otherTypes;

    public CatchType (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	firstType = (UnannClassType)children.get (0);
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    otherTypes = new ArrayList<> ();
	    for (int j = 1, e = z.size (); j < e; j += 2)
		otherTypes.add ((ClassType)z.get (j));
	} else {
	    otherTypes = List.of ();
	}
    }

    @Override public Object getValue () {
	if (otherTypes.isEmpty ())
	    return firstType.toString ();

	StringBuilder sb = new StringBuilder ();
	sb.append (firstType);
	for (ClassType c : otherTypes)
	    sb.append (" | ").append (c);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (firstType);
	otherTypes.forEach (v::accept);
    }
}
