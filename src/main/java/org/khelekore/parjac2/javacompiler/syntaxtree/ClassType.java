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

    public ClassType (String fqn) {
	super (null);
	types = List.of (new SimpleClassType (null, fqn, null));
	this.fqn = fqn;
    }

    public void add (SimpleClassType sct) {
	types.add (sct);
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

    public String getSlashName () {
	if (fqn == null) throw new IllegalStateException ("FQN not set");
	return fqn.replace ('.', '/'); // TODO: probably not correct now since we do not have $ for inner classes
    }

    public TypeArguments getTypeArguments () {
	List<SimpleClassType> ls = get ();
	return ls.get (ls.size () - 1).getTypeArguments ();
    }

    public ExpressionType getExpressionType () {
	if (fqn != null)
	    return ExpressionType.getObjectType (fqn);
	return null;
    }
}
