package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.javacompiler.VariableInfo;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FieldAccess extends SyntaxTreeNode {
    private final ParseTreeNode from;
    private final boolean isSuper;
    private final String id;
    private VariableInfo vi;

    public FieldAccess (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	if (rule.get (0) != ctx.getTokens ().SUPER.getId ()) {
	    from = children.get (0);
	    isSuper = rule.size () > 3;
	} else {
	    from = null;
	    isSuper = true;
	}
	id = ((Identifier)children.get (children.size () - 1)).getValue ();
    }

    public FieldAccess (ParsePosition pos, ParseTreeNode from, String id) {
	super (pos);
	this.from = from;
	this.isSuper = false;
	this.id = id;
    }

    public ParseTreeNode from () {
	return from;
    }

    public String name () {
	return id;
    }

    public void variableInfo (VariableInfo vi) {
	this.vi = vi;
    }

    public VariableInfo variableInfo () {
	return vi;
    }

    public FullNameHandler getFullName () {
	return FullNameHelper.type (vi.type ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (from != null)
	    sb.append (from);
	if (isSuper) {
	    if (!sb.isEmpty ())
		sb.append (".");
	    sb.append ("super");
	}
	if (!sb.isEmpty ())
	    sb.append (".");
	sb.append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (from != null)
	    v.accept (from);
    }
}
