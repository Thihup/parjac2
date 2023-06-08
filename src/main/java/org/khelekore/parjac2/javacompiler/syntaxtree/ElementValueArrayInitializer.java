package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ElementValueArrayInitializer extends SyntaxTreeNode {
    private final List<ParseTreeNode> values;

    public ElementValueArrayInitializer (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (children.get (1) instanceof ElementValueList)
	    values = ((ElementValueList)children.get (1)).getValues ();
	else
	    values = null;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	if (values != null)
	    sb.append (values);
	sb.append ("}");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	values.forEach (v::accept);
    }
}
