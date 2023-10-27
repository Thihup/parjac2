package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class PackageNameTo<T extends DottedName, S extends DottedName> extends ModuleDirective {
    private T packageName;
    private List<S> exportedTo;

    @SuppressWarnings("unchecked")
    public PackageNameTo (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	packageName = (T)children.get (1);
	if (r.size () > 3) {
	    exportedTo = new ArrayList<> ();
	    exportedTo.add ((S)children.get (3));
	    if (r.size () > 5) {
		Multiple z = (Multiple)children.get (4);
		for (int i = 1, e = z.size (); i < e; i += 2)
		    exportedTo.add ((S)z.get (i));
	    }
	}
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (getType ()).append (" ").append (packageName);
	if (exportedTo != null) {
	    sb.append (" ").append (getThing ()).append (" " ).append (exportedTo.get (0));
	    for (int i = 1; i < exportedTo.size (); i++)
		sb.append (", " + exportedTo.get (i));
	}
	sb.append (";");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (packageName);
	exportedTo.forEach (v::accept);
    }

    protected abstract String getType ();
    protected abstract String getThing ();
}
