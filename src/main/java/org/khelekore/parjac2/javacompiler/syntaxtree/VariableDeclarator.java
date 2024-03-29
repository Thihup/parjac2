package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableDeclarator extends SyntaxTreeNode {
    private final VariableDeclaratorId id;
    private final ParseTreeNode initializer;
    private ParseTreeNode type;
    private int localSlot = -1;

    public VariableDeclarator (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	id = (VariableDeclaratorId)children.get (0);
	initializer = rule.size () > 1 ? children.get (2) : null;
    }

    public VariableDeclarator (VariableDeclaratorId id) {
	super (id.position ());
	this.id = id;
	initializer = null;
    }

    public VariableDeclarator (ParsePosition pos, String name, ParseTreeNode initializer) {
	super (pos);
	id = new VariableDeclaratorId (pos, name, 0);
	this.initializer = initializer;
    }

    public void type (ParseTreeNode type) {
	this.type = type;
    }

    public ParseTreeNode type () {
	return type;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (id);
	if (initializer != null)
	    sb.append (" = ").append (initializer.getValue ());
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (id);
	if (initializer != null)
	    v.accept (initializer);
    }

    public String getName () {
	return id.name ();
    }

    public boolean isArray () {
	return id.isArray ();
    }

    public Dims getDims () {
	return id.dims ();
    }

    public int rank () {
	return id.rank ();
    }

    public boolean hasInitializer () {
	return initializer != null;
    }

    public ParseTreeNode initializer () {
	return initializer;
    }

    public void localSlot (int localSlot) {
	this.localSlot = localSlot;
    }

    public int slot () {
	if (localSlot == -1)
	    throw new IllegalStateException ("Local slot position not set!");
	return localSlot;
    }
}
