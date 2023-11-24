package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class LambdaParameters extends SyntaxTreeNode {
    public LambdaParameters (ParsePosition pos) {
	super (pos);
    }

    public abstract int numberOfArguments ();

    public abstract String parameterName (int i);

    public abstract FullNameHandler parameter (int i);
}
