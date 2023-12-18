package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

/** Base class for pre and post increment and decrement classes
 */
public abstract class ChangeByOneExpression extends SyntaxTreeNode {
    protected final ParseTreeNode expression;
    private boolean valueIsUsed = true;

    public ChangeByOneExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children, int expressionPos) {
	super (n.position ());
	expression = children.get (expressionPos);
    }

    public ParseTreeNode expression () {
	return expression;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (expression);
    }

    public void valueIsUsed (boolean flag) {
	valueIsUsed = flag;
    }

    public boolean valueIsUsed () {
	return valueIsUsed;
    }
}
