package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class AnonymousClass extends TypeDeclaration {
    private String anonymousClassname;

    public AnonymousClass (ParsePosition pos) {
	super (pos);
    }

    public void setAnonymousClassname (String anonymousClassname) {
	this.anonymousClassname = anonymousClassname;
    }

    @Override public String getName () {
	return anonymousClassname;
    }

    @Override public TypeParameters getTypeParameters () {
	return null;
    }
}
