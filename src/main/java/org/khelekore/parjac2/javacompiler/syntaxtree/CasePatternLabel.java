package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CasePatternLabel extends SwitchLabel {
    private final ParseTreeNode pattern;
    private final Guard guard;

    public CasePatternLabel (ParsePosition pos, List<ParseTreeNode> children) {
	super (pos);
	pattern = children.get (1);
	guard = children.size () > 2 ? (Guard)children.get (2) : null;
}

    @Override public Object getValue () {
	return "case " + pattern + (guard != null ? guard : "");
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (pattern);
	if (guard != null)
	    v.accept (guard);
    }
}
