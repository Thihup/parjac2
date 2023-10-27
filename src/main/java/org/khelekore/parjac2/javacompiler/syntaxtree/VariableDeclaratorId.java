package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableDeclaratorId extends SyntaxTreeNode {
    private String id;
    private Dims dims;

    public VariableDeclaratorId (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	id = ((Identifier)children.get (0)).getValue ();
	if (rule.size () > 1)
	    dims = (Dims)children.get (1);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (id);
	if (dims != null)
	    sb.append (dims);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (dims != null)
	    v.accept (dims);
    }

    public String getName () {
	return id;
    }

    public boolean isArray () {
	return dims != null;
    }

    public Dims getDims () {
	return dims;
    }
}
