package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;

public class ClassType extends SyntaxTreeNode {
    private final List<SimpleClassType> types;
    private FullNameHandler fnh;
    private TypeParameter tp;

    public ClassType (ParsePosition pos, List<SimpleClassType> types) {
	super (pos);
	this.types = types;
    }

    public ClassType (FullNameHandler fnh) {
	super (null);
	types = List.of (new SimpleClassType (null, fnh.getFullDotName (), null)); // TODO: do we need this?
	this.fnh = fnh;
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

    public void setFullName (FullNameHandler fnh) {
	this.fnh = fnh;
    }

    public FullNameHandler getFullNameHandler () {
	return fnh;
    }

    public boolean hasFullName () {
	return fnh != null;
    }

    public String getFullDotName () {
	return fnh.getFullDotName ();
    }

    public String getFullDollarName () {
	return fnh.getFullDollarName ();
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
	if (fnh == null) throw new IllegalStateException ("FQN not set");
	return fnh.getSlashName ();
    }

    public TypeArguments getTypeArguments () {
	List<SimpleClassType> ls = get ();
	return ls.get (ls.size () - 1).getTypeArguments ();
    }

    public ExpressionType getExpressionType () {
	if (fnh != null)
	    return ExpressionType.getObjectType (fnh);
	return null;
    }

    public FullNameHandler getFullNameAsSimpleDottedName () {
	String dotName;
	if (size () == 1)
	    dotName = get ().get (0).getId ();
	else
	    dotName = get ().stream ().map (s -> s.getId ()).collect (Collectors.joining ("."));
	return FullNameHandler.ofSimpleClassName (dotName);
    }
}
