package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchLabelColon extends SyntaxTreeNode {
    private SwitchLabel label;

    public SwitchLabelColon (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	label = (SwitchLabel)children.get (0);
    }

    public SwitchLabel label () {
	return label;
    }

    @Override public Object getValue () {
	return label.getValue () + ": ";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (label);
    }
}
