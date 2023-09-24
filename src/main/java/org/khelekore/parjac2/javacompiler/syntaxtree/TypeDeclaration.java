package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class TypeDeclaration extends FlaggedBase {
    protected int flags;
    protected String localName;

    public TypeDeclaration (ParsePosition pos) {
	super (pos);
    }

    /** Get the class/enum/interface/annotation name of this type */
    public abstract String getName ();

    /** Get all the inner classes, enums, interfaces and annotations */
    public abstract List<TypeDeclaration> getInnerClasses ();

    /** Check if the given type is a local class for this class */
    public abstract boolean isLocalClass (TypeDeclaration td);

    public boolean isLocalClass () {
	return localName != null;
    }

    public void setLocalName (String localName) {
	this.localName = localName;
    }

    public String getLocalName () {
	return localName;
    }
}
