package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FieldDeclaration extends ClassBodyDeclaration {
    private List<ParseTreeNode> modifiers;
    private ParseTreeNode type;
    private VariableDeclaratorList list;

    public FieldDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	modifiers = (rule.size () > 3) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	type = children.get (i++);
	list = (VariableDeclaratorList)children.get (i++);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (list).append (";");
	return sb.toString ();
    }
}
