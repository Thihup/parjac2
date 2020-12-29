package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final ConstructorDeclarator declarator;
    private final Throws t;
    private final ConstructorBody body;
    public ConstructorDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    modifiers = ((Multiple)children.get (i++)).get ();
	else
	    modifiers = Collections.emptyList ();
	declarator = (ConstructorDeclarator)children.get (i++);
	t = (rule.size () > i + 1) ? (Throws)children.get (i++) : null;
	body = (ConstructorBody)children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (declarator).append (" ");
	if (t != null)
	    sb.append (t).append (" ");
	sb.append (body);
	return sb.toString ();
    }
}
