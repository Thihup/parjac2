package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchBlockStatementGroup extends SyntaxTreeNode {
    private final List<SwitchLabel> labels = new ArrayList<> ();
    private final BlockStatements statements;

    public SwitchBlockStatementGroup (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	// SwitchLabel ':' {SwitchLabel ':'} BlockStatements
	labels.add ((SwitchLabel)children.get (0));
	int i = 2;
	if (children.get (i) instanceof Multiple m) {
	    i++;
	    for (int j = 0; j < m.size (); j += 2)
		labels.add ((SwitchLabel)m.get (j));
	}
	statements = (BlockStatements)children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	for (SwitchLabel l : labels)
	    sb.append (l).append (" ");
	sb.append (statements);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	labels.forEach (v::accept);
	v.accept (statements);
    }
}
