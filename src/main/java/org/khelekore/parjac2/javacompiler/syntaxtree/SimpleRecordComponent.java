package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SimpleRecordComponent extends SyntaxTreeNode {

    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final String id;

    public SimpleRecordComponent (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	modifiers = (rule.size () > 2) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	type = children.get (i++);
	id = ((Identifier)children.get (i++)).getValue ();
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	modifiers.forEach (m -> sb.append (m.getValue ()).append (" "));
	sb.append (type.getValue ()).append (" ").append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
    }
}
