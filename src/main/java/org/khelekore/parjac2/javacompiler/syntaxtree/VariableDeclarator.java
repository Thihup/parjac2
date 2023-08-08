package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableDeclarator extends SyntaxTreeNode {
    private final VariableDeclaratorId id;
    private final ParseTreeNode initializer;

    public VariableDeclarator (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	id = (VariableDeclaratorId)children.get (0);
	initializer = rule.size () > 1 ? children.get (2) : null;
    }

    public VariableDeclarator (VariableDeclaratorId id) {
	super (id.getPosition ());
	this.id = id;
	initializer = null;
    }

    @Override public Object getValue() {
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
	return id.getName ();
    }

    public boolean isArray () {
	return id.isArray ();
    }

    public Dims getDims () {
	return id.getDims ();
    }

    public boolean hasInitializer () {
	return initializer != null;
    }

    public ParseTreeNode getInitializer () {
	return initializer;
    }
}
