package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class ConstructorBase extends FlaggedBase implements ConstructorDeclarationInfo {
    private FullNameHandler owner;

    public ConstructorBase (ParsePosition pos) {
	super (pos);
    }

    @Override public void owner (FullNameHandler owner) {
	this.owner = owner;
    }

    @Override public FullNameHandler owner () {
	return owner;
    }
}
