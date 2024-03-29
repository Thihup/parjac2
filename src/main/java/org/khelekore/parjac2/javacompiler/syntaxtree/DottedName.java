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

public class DottedName extends SyntaxTreeNode implements NamePartHandler {
    private final List<String> nameParts;
    private FullNameHandler fnh;
    private ParseTreeNode replaced;

    public DottedName (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
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

    public void replace (ParseTreeNode replaced) {
	this.replaced = replaced;
    }

    public ParseTreeNode replaced () {
	return replaced == null ? this : replaced;
    }

    @Override public Object getValue () {
	return StringHelper.dotted (nameParts);
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (replaced != null)
	    replaced.visitChildNodes (v);
	// else: no children
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	DottedName dn = (DottedName)o;
	return Objects.equals (replaced, dn.replaced) && Objects.equals (nameParts, dn.nameParts);
    }

    public List<String> getParts () {
	return nameParts;
    }

    public DottedName allButLast () {
	List<String> ls = getParts ().subList (0, size () - 1);
	return new DottedName (position (), ls);
    }

    public String getLastPart () {
	return nameParts.get (nameParts.size () - 1);
    }

    public String getDotName () {
	return StringHelper.dotted (nameParts);
    }

    @Override public int size () {
	return nameParts.size ();
    }

    @Override public String getNamePart (int i) {
	return nameParts.get (i);
    }

    public void fullName (FullNameHandler fnh) {
	this.fnh = fnh;
    }

    public FullNameHandler fullName () {
	return fnh;
    }

    public boolean hasFullName () {
	return fnh != null;
    }

    public FullNameHandler getFullNameAsSimpleDottedName () {
	String dotName = String.join (".", nameParts);
	return FullNameHandler.ofSimpleClassName (dotName);
    }

    public FullNameHandler dotName (int upToAndIncluding) {
	String dotName = String.join (".", nameParts.subList (0, upToAndIncluding + 1));
	return FullNameHandler.ofSimpleClassName (dotName);
    }
}
