package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CaseLabel extends SwitchLabel {
    private final ParseTreeNode expression;
    public CaseLabel (ParsePosition pos, ParseTreeNode expression) {
	super (pos);
	this.expression = expression;
    }

    @Override public Object getValue () {
	return "case " + expression.getValue ()  + ":";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (expression);
    }
}
