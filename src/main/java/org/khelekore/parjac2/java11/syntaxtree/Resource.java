package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Resource extends SyntaxTreeNode {
    private List<ParseTreeNode> modifiers;
    private ParseTreeNode type;
    private String id;
    private ParseTreeNode expression;

    public Resource (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	modifiers = rule.size () > 4 ? ((Multiple)children.get (i++)).get () : List.of ();
	type = children.get (i++);
	id = ((Identifier)children.get (i++)).getValue ();
	i++;
	expression = children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    modifiers.forEach (m -> sb.append (m).append (" "));
	sb.append (type).append (id).append (" = ").append (expression);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	v.accept (expression);
    }
}
