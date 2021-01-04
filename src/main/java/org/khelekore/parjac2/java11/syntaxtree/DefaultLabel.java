package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;

public class DefaultLabel extends SwitchLabel {
    public DefaultLabel (ParsePosition pos) {
	super (pos);
    }

    @Override public Object getValue () {
	return "default:";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// empty
    }
}
