package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchRule extends SyntaxTreeNode {
    private final SwitchLabel label;
    private final ParseTreeNode handler;

    public SwitchRule (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	label = (SwitchLabel)children.get (0);
	handler = children.get (2);
    }
    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (label.getValue ()).append ("->").append (handler.getValue ());
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (label);
	v.accept (handler);
    }
}
