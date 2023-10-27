package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchBlockStatementGroup extends SyntaxTreeNode {
    private final List<SwitchLabelColon> labels;
    private final BlockStatements statements;

    public SwitchBlockStatementGroup (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	// SwitchLabels BlockStatements
	labels = ((SwitchLabels)children.get (0)).getLabels ();
	statements = (BlockStatements)children.get (1);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	for (SwitchLabelColon l : labels)
	    sb.append (l).append (" ");
	sb.append (statements);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	labels.forEach (v::accept);
	v.accept (statements);
    }
}
