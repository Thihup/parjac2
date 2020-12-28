package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorDeclarator extends SyntaxTreeNode {
    private TypeParameters types;
    private String id;
    private ReceiverParameter rp;
    private FormalParameterList params;
    public ConstructorDeclarator (Path path, Grammar grammar, Rule rule,
				  ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.get (i) == grammar.getRuleGroupId ("TypeParameters"))
	    types = (TypeParameters)children.get (i++);
	id = ((Identifier)children.get (i++)).getValue ();
	if (rule.get (i) == grammar.getRuleGroupId ("ReceiverParameter")) {
	    rp = (ReceiverParameter)children.get (i);
	    i += 2;
	}
	if (rule.get (i) == grammar.getRuleGroupId ("FormalParameterList"))
	    params = (FormalParameterList)children.get (i++);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (types != null)
	    sb.append (types).append (" ");
	sb.append (id).append (" (");
	if (rp != null) {
	    sb.append (rp);
	    if (params != null)
		sb.append (", ");
	}
	if (params != null)
	    sb.append (params);
	sb.append (")");
	return sb.toString ();
    }
}
