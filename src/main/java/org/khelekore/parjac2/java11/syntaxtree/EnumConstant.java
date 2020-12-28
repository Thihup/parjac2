package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.java11.Java11Tokens;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumConstant extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final ArgumentList args;
    private final ClassBody body;
    public EnumConstant (Path path, Java11Tokens java11Tokens, Rule rule,
			 ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	id = ((Identifier)children.get (i++)).getValue ();
	if (rule.size () > i && rule.get (i++) == java11Tokens.LEFT_PARENTHESIS.getId ()) {
	    if (rule.size () > i && rule.get (i) != java11Tokens.RIGHT_PARENTHESIS.getId ()) {
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
}
