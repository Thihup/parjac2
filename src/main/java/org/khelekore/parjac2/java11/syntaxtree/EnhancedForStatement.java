package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnhancedForStatement extends SyntaxTreeNode {
    private List<ParseTreeNode> modifiers;
    private ParseTreeNode type;
    private VariableDeclaratorId id;
    private ParseTreeNode expression;
    private ParseTreeNode statement;

    public EnhancedForStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 2;
	modifiers = rule.size () > 8 ? ((Multiple)children.get (i++)).get () : List.of ();
	type = children.get (i++);
	id = (VariableDeclaratorId)children.get (i++);
	i++; // :
	expression = children.get (i++);
	i++;
	statement = children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("for (");
	modifiers.forEach (m -> sb.append (m).append (" "));
	sb.append (type).append (" ").append (id).append (" : ").append (expression).append (")")
	    .append (statement);
	return sb.toString ();
    }
}
