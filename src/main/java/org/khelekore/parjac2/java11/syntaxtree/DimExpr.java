package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DimExpr extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final ParseTreeNode expression;

    public DimExpr (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.size () > 3)
	    annotations = ((Multiple)children.get (i++)).get ();
	else
	    annotations = List.of ();
	expression = children.get (i + 1);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations);
	sb.append ("[").append (expression).append ("]");
	return sb.toString ();
    }
}
