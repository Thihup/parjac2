package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayCreationExpression extends SyntaxTreeNode {
    private final SyntaxTreeNode type;
    private final DimExprs dimExprs;
    private final Dims dims;
    private final ArrayInitializer initializer;

    public ArrayCreationExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 1;
	type = (SyntaxTreeNode)children.get (i++);
	dimExprs = (children.get (i) instanceof DimExprs) ? (DimExprs)children.get (i++) : null;
	dims = (children.size () > i) ? (Dims)children.get (i++) : null;
	initializer = (children.size () > i) ? (ArrayInitializer)children.get (i++) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("new ");
	sb.append (type);
	if (dimExprs != null)
	    sb.append (dimExprs);
	if (dims != null)
	    sb.append (dims);
	if (initializer != null)
	    sb.append (initializer);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (type);
	if (dimExprs != null)
	    v.accept (dimExprs);
	if (dims != null)
	    v.accept (dims);
	if (initializer != null)
	    v.accept (initializer);
    }
}
