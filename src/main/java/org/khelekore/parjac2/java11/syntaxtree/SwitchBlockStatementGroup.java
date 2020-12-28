package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchBlockStatementGroup extends SyntaxTreeNode {
    private List<SwitchLabel> labels;
    private BlockStatements statements;

    public SwitchBlockStatementGroup (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	SwitchLabels l = (SwitchLabels)children.get (0);
	labels = l.getLabels ();
	statements = (BlockStatements)children.get (1);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	for (SwitchLabel l : labels)
	    sb.append (l).append (" ");
	sb.append (statements);
	return sb.toString ();
    }
}
