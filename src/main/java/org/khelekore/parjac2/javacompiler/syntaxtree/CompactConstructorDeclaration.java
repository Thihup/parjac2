package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CompactConstructorDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final String name;
    private final ConstructorBody body;

    public CompactConstructorDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	// {ConstructorModifier} SimpleTypeName ConstructorBody
	int i = 0;
	modifiers = (rule.size () > 2) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	name = ((Identifier)children.get (i++)).getValue ();
	body = (ConstructorBody)children.get (i++);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	modifiers.forEach (m -> sb.append (m).append (" "));
	sb.append (name).append (" ").append (body.getValue ());
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (body);
    }
}
