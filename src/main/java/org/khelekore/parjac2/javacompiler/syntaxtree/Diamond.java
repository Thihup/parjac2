package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;

public class Diamond extends SyntaxTreeNode {
    public Diamond (ParsePosition pos) {
	super (pos);
    }

    @Override public Object getValue () {
	return "<>";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// empty
    }
}
