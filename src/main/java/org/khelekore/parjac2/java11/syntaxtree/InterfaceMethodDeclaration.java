package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class InterfaceMethodDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final MethodHeader header;
    private final ParseTreeNode body;
    public InterfaceMethodDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.size () > 2){
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	header = (MethodHeader)children.get (i++);
	body = children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (header).append (body);
	return sb.toString ();
    }
}
