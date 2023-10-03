package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.StringHelper;

public class DottedName extends SyntaxTreeNode {
    private final List<String> nameParts;

    public DottedName (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () == 1) {
	    nameParts = new ArrayList<> ();
	    nameParts.add (((Identifier)children.get (0)).getValue ());
	} else {
	    DottedName pot = (DottedName)children.get (0);
	    nameParts = pot.nameParts;
	    nameParts.add (((Identifier)children.get (2)).getValue ());
	}
    }

    public DottedName (ParsePosition pos, List<String> parts) {
	super (pos);
	this.nameParts = new ArrayList<> (parts);
    }

    @Override public Object getValue () {
	return StringHelper.dotted (nameParts);
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// empty
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	DottedName dn = (DottedName)o;
	return Objects.equals (nameParts, dn.nameParts);
    }

    public List<String> getParts () {
	return nameParts;
    }

    public String getLastPart () {
	return nameParts.get (nameParts.size () - 1);
    }

    public String getDotName () {
	return StringHelper.dotted (nameParts);
    }
}
