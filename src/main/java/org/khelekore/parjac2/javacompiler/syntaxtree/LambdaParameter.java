package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class LambdaParameter extends SyntaxTreeNode {
    public LambdaParameter (ParsePosition pos) {
	super (pos);
    }

    public abstract String name ();

    public abstract FullNameHandler type ();
}
