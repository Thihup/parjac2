package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression;
    private final SwitchBlock block;

    public SwitchStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	expression = children.get (2);
	block = (SwitchBlock)children.get (4);
    }

    @Override public Object getValue () {
	return "switch (" + expression + ")" + block;
    }
}
