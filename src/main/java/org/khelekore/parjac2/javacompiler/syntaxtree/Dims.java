package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Dims extends SyntaxTreeNode {
    // one list of annotations per dim
    List<List<ParseTreeNode>> annotations;
    public Dims (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	annotations = new ArrayList<> ();
	int i = 0;
	if (children.get (0) instanceof Multiple)
	    annotations.add (((Multiple)children.get (i++)).get ());
	else
	    annotations.add (Collections.emptyList ());
	if (rule.size () > i + 2) {
	    Multiple z = (Multiple)children.get (i + 2);
	    for (int j = 0, e = z.size (); j < e; j += 2) {
		if (z.get (j) instanceof Multiple)
		    annotations.add (((Multiple)z.get (j++)).get ());
		else
		    annotations.add (Collections.emptyList ());
	    }
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	for (List<ParseTreeNode> ls : annotations) {
	    if (!ls.isEmpty ())
		sb.append (ls.toString ());
	    sb.append ("[]");
	}
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (ls -> ls.forEach (v::accept));
    }

    public int rank () {
	return annotations.size ();
    }
}
