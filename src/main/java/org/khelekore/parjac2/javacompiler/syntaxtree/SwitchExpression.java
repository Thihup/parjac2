package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchExpression extends SwitchExpressionOrStatement {
    private FullNameHandler type;

    public SwitchExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (rule, n, children);
    }

    public void type (FullNameHandler type) {
	this.type = type;
    }

    public FullNameHandler type () {
	return type;
    }
}
