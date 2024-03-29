package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayCreationExpression extends SyntaxTreeNode {
    private final ParseTreeNode type;
    private final DimExprs dimExprs;
    private final Dims dims;
    private final ArrayInitializer initializer;

    public ArrayCreationExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 1;
	type = children.get (i++);
	dimExprs = (children.get (i) instanceof DimExprs) ? (DimExprs)children.get (i++) : null;
	dims = (children.size () > i) ? (Dims)children.get (i++) : null;
	initializer = (children.size () > i) ? (ArrayInitializer)children.get (i++) : null;
    }

    public ArrayCreationExpression (ParsePosition pos, ParseTreeNode type, DimExprs dims, List<ParseTreeNode> initializerParts) {
	super (pos);
	this.type = type;
	this.dimExprs = dims;
	this.dims = null;
	this.initializer = new ArrayInitializer (initializerParts);
    }

    public FullNameHandler fullName () {
	return FullNameHelper.type (type).array (rank ());
    }

    public FullNameHandler innerFullName () {
	return FullNameHelper.type (type);
    }

    public int rank () {
	int r = 0;
	if (dimExprs != null)
	    r += dimExprs.rank ();
	if (dims != null)
	    r += dims.rank ();
	return r;
    }

    public int dimExprsRank () {
	return dimExprs != null ? dimExprs.rank () : 0;
    }

    public DimExprs dimExprs () {
	return dimExprs;
    }

    public Dims dims () {
	return dims;
    }

    public ArrayInitializer initializer () {
	return initializer;
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
