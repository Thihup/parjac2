package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DimExpr extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final ParseTreeNode expression;

    public DimExpr (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	if (rule.size () > 3)
	    annotations = ((Multiple)children.get (i++)).get ();
	else
	    annotations = List.of ();
	expression = children.get (i + 1);
    }

    public DimExpr (ParsePosition pos, ParseTreeNode expression) {
	super (pos);
	annotations = List.of ();
	this.expression = expression;
    }

    public ParseTreeNode expression () {
	return expression;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations);
	sb.append ("[").append (expression).append ("]");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (v::accept);
	v.accept (expression);
    }
}
