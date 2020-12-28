package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class InterfaceBody extends SyntaxTreeNode {
    private final List<ParseTreeNode> memberDeclarations;
    public InterfaceBody (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 2) {
	    memberDeclarations = ((Multiple)children.get (1)).get ();
	} else {
	    memberDeclarations = Collections.emptyList ();
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (" {\n");
	if (!memberDeclarations.isEmpty ())
	    for (ParseTreeNode n : memberDeclarations)
		sb.append (n).append ("\n");
	sb.append ("}");
	return sb.toString ();
    }
}
