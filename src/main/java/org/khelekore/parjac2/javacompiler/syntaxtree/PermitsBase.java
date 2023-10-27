package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class PermitsBase extends SyntaxTreeNode {
    private List<TypeName> permits = new ArrayList<> ();

    public PermitsBase (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	permits.add ((TypeName)children.get (1));
	if (rule.size () > 2) {
	    Multiple z = (Multiple)children.get (2);
	    for (int i = 1, e = z.size (); i < e; i += 2)
		permits.add ((TypeName)z.get (i));
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("permits ");
	sb.append (permits.get (0).getValue ());
	for (int i = 1; i < permits.size (); i++) {
	    sb.append (", ");
	    sb.append (permits.get (i).getValue ());
	}
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	permits.forEach (v::accept);
    }

    public List<TypeName> getPermittedTypes () {
	return permits;
    }
}
