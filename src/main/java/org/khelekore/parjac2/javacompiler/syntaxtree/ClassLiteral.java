package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ClassLiteral extends SyntaxTreeNode {
    private final ParseTreeNode type;
    private final int dims;
    public ClassLiteral (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	type = children.get (i++);
	if (children.get (i) instanceof Multiple)
	    dims = ((Multiple)children.get (i++)).size ();
	else
	    dims = 0;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (type);
	for (int i = 0; i < dims; i++)
	    sb.append ("[]");
	sb.append (".class");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (type);
    }
}
