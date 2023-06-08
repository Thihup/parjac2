package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DefaultValue extends SyntaxTreeNode {
    private ParseTreeNode value;
    public DefaultValue (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	this.value = children.get (1);
    }

    @Override public Object getValue() {
	return "default " + value;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (value);
    }
}
