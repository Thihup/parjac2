package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class TypeDeclaration extends FlaggedBase {
    protected int flags;

    public TypeDeclaration (ParsePosition pos) {
	super (pos);
    }

    /** Get the class/enum/interface/annotation name of this type */
    public abstract String getName ();

    /** Get all the inner classes, enums, interfaces and annotations */
    public abstract List<TypeDeclaration> getInnerClasses ();
}
