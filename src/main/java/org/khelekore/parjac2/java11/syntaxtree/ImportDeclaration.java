package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class ImportDeclaration extends SyntaxTreeNode {
    public ImportDeclaration (ParsePosition pos) {
	super (pos);
    }
}
