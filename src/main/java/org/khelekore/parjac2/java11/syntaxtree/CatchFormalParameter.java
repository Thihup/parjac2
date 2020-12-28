package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CatchFormalParameter extends SyntaxTreeNode {
    private final List<ParseTreeNode> variableModifiers;
    private final CatchType type;
    private final VariableDeclaratorId vid;

    public CatchFormalParameter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	variableModifiers = rule.size () > 2 ? ((Multiple)children.get (i++)).get () : List.of ();
	type = (CatchType)children.get (i++);
	vid = (VariableDeclaratorId)children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!variableModifiers.isEmpty ())
	    sb.append (variableModifiers).append (" ");
	sb.append (type).append (" ").append (vid);
	return sb.toString ();
    }
}
