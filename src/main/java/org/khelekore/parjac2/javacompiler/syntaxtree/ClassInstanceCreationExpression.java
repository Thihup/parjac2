package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ClassInstanceCreationExpression extends SyntaxTreeNode {
    private ExpressionName name;
    private ParseTreeNode primary;
    private UnqualifiedClassInstanceCreationExpression exp;

    public ClassInstanceCreationExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	ParseTreeNode tn = children.get (0);
	if (children.size () > 1) {
	    if (tn instanceof ExpressionName) {
		name = (ExpressionName)tn;
	    } else {
		primary = tn;
	    }
	    exp = (UnqualifiedClassInstanceCreationExpression)children.get (2);
	} else {
	    exp = (UnqualifiedClassInstanceCreationExpression)tn;
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (name != null)
	    sb.append (name).append (".");
	if (primary != null)
	    sb.append (primary).append (".");
	sb.append (exp);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (name != null)
	    v.accept (name);
	if (primary != null)
	    v.accept (primary);
	v.accept (exp);
    }
}
