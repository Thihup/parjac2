package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LambdaExpression extends SyntaxTreeNode {
    private LambdaParameters params;
    private ParseTreeNode body;

    public LambdaExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	params = (LambdaParameters)children.get (0);
	body = children.get (2);
    }

    @Override public Object getValue () {
	return params + " -> " + body;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (params);
	v.accept (body);
    }
}
