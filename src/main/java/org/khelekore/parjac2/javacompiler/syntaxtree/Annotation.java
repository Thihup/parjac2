package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class Annotation extends SyntaxTreeNode {
    public Annotation (ParsePosition pos) {
	super (pos);
    }
}
