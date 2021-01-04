package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableDeclarator extends SyntaxTreeNode {
    private VariableDeclaratorId id;
    private ParseTreeNode initializer;

    public VariableDeclarator (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	id = (VariableDeclaratorId)children.get (0);
	if (rule.size () > 1)
	    initializer = children.get (2);
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
}
