package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LambdaExpression extends SyntaxTreeNode {
    private LambdaParameters params;
    private ParseTreeNode body;

    public LambdaExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	params = (LambdaParameters)children.get (0);
	body = children.get (2);
    }

    @Override public Object getValue () {
	return params + " -> " + body;
    }
}
