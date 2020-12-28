package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class ModuleDirective extends SyntaxTreeNode {
    public ModuleDirective (ParsePosition pos) {
	super (pos);
    }
}
