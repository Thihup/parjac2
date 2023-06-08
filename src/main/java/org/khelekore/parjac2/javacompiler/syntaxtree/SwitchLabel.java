package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class SwitchLabel extends SyntaxTreeNode {
    public SwitchLabel (ParsePosition pos) {
	super (pos);
    }
}
