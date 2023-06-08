package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class ImportDeclaration extends SyntaxTreeNode {
    public ImportDeclaration (ParsePosition pos) {
	super (pos);
    }
}
