package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ReturnStatement extends SyntaxTreeNode {
    private final ParseTreeNode expression;
    private FullNameHandler fn;

    public ReturnStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	expression = rule.size () > 2 ? children.get (1) : null;
    }

    public ReturnStatement (ParsePosition pos, ParseTreeNode expression) {
	super (pos);
	this.expression = expression;
    }

    public ReturnStatement (ParsePosition pos, String field) {
	super (pos);
	expression = new ExpressionName (pos, field);
    }

    @Override public Object getValue () {
	return expression != null ? "return " + expression.getValue () + ";" : "return;";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (expression != null)
	    v.accept (expression);
    }

    public ParseTreeNode expression () {
	return expression;
    }

    public void type (FullNameHandler fn) {
	this.fn = fn;
    }

    public FullNameHandler type () {
	return fn;
    }
}
