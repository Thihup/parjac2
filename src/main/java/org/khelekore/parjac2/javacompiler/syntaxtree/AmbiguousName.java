package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;
import java.util.Objects;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class AmbiguousName extends DottedName {
    private ParseTreeNode replaced;

    public AmbiguousName (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (rule, n, children);
    }

    public AmbiguousName (ParsePosition pos, List<String> parts) {
	super (pos, parts);
    }

    public void replace (ParseTreeNode replaced) {
	this.replaced = replaced;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (replaced != null)
	    replaced.visitChildNodes (v);
	else
	    super.visitChildNodes (v);
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	AmbiguousName dn = (AmbiguousName)o;
	return Objects.equals (replaced, dn.replaced) && Objects.equals (getParts (), dn.getParts ());
    }
}
