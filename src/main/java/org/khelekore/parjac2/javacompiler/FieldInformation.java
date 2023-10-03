package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.ExpressionType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Flagged;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FieldInformation<T extends Flagged> {
    private final String name;
    private final T var;
    private final ParseTreeNode owner;
    private final boolean initialized;

    public FieldInformation (String name, T var, ParseTreeNode owner, boolean initialized) {
	this.name = name;
	this.var = var;
	this.owner = owner;
	this.initialized = initialized;
    }

    @Override public String toString () {
	return getClass ().getSimpleName () + "{name: " + name + ", var: " + var +
	    ", owner: " + owner.getClass ().getName () + "}";
    }

    public String getName () {
	return name;
    }

    public ParseTreeNode getOwner () {
	return owner;
    }

    public T getVariableDeclaration () {
	return var;
    }

    public ParsePosition getParsePosition () {
	return var.getPosition ();
    }

    public boolean isInitialized () {
	return initialized;
    }

    public boolean isStatic () {
	return Flags.isStatic (var.getFlags ());
    }

    public boolean isFinal () {
	return Flags.isFinal (var.getFlags ());
    }

    public ExpressionType getExpressionType () {
	return var.getExpressionType ();
    }
}
