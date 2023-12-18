package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class PreDecrementExpression extends ChangeByOneExpression {
    public PreDecrementExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (rule, n, children, 1);
    }

    @Override public Object getValue() {
	return expression + "--";
    }
}
