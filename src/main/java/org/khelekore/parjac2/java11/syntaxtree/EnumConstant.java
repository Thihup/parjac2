package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumConstant extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final ArgumentList args;
    private final ClassBody body;

    public EnumConstant (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	id = ((Identifier)children.get (i++)).getValue ();
	if (rule.size () > i && rule.get (i++) == ctx.getTokens ().LEFT_PARENTHESIS.getId ()) {
	    if (rule.size () > i && rule.get (i) != ctx.getTokens ().RIGHT_PARENTHESIS.getId ()) {
		args = (ArgumentList)children.get (i++);
	    } else {
		args = null;
	    }
	    i++; // ')'
	} else {
	    args = null;
	}
	body = (rule.size () > i) ? (ClassBody)children.get (i) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (id);
	if (args != null)
	    sb.append ("(").append (args).append (")");
	if (body != null)
	    sb.append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (args);
	v.accept (body);
    }
}
