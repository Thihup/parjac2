package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;

public class ClassType extends SyntaxTreeNode {
    private final List<SimpleClassType> types;
    private String fqn;
    private TypeParameter tp;

    public ClassType (ParsePosition pos, List<SimpleClassType> types) {
	super (pos);
	this.types = types;
    }

    @Override public Object getValue () {
	return types.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	types.forEach (v::accept);
    }

    public List<SimpleClassType> getTypes () {
	return types;
    }

    public void setFullName (String fqn) {
	this.fqn = fqn;
    }

    public String getFullName () {
	return fqn;
    }

    public void setTypeParameter (TypeParameter tp) {
	this.tp = tp;
    }

    public TypeParameter getTypeParameter () {
	return tp;
    }

    public int size () {
	return types.size ();
    }

    public List<SimpleClassType> get () {
	return types;
    }

    public ExpressionType getExpressionType () {
	if (fqn != null)
	    return ExpressionType.getObjectType (fqn);
	return null;
    }
}
