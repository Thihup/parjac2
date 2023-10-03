package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract sealed class ImportDeclaration extends SyntaxTreeNode
    permits SingleTypeImportDeclaration, TypeImportOnDemandDeclaration,
	    SingleStaticImportDeclaration, StaticImportOnDemandDeclaration {
    private boolean used = false;

    public ImportDeclaration (ParsePosition pos) {
	super (pos);
    }

    /** Mark this import clause as used */
    public void markUsed () {
	used = true;
    }

    /** Check if this import clause is actually used */
    public boolean hasBeenUsed () {
	return used;
    }
}
