package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchExpressionOrStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression;
    private final SwitchBlock block;

    public SwitchExpressionOrStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	expression = children.get (2);
	block = (SwitchBlock)children.get (4);
    }

    public ParseTreeNode expression () {
	return expression;
    }

    public SwitchBlock block () {
	return block;
    }

    @Override public Object getValue () {
	return "switch (" + expression + ")" + block;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (expression);
	v.accept (block);
    }
}
