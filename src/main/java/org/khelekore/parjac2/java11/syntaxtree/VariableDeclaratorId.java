package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableDeclaratorId extends SyntaxTreeNode {
    private String id;
    private Dims dims;

    public VariableDeclaratorId (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	id = ((Identifier)children.get (0)).getValue ();
	if (rule.size () > 1)
	    dims = (Dims)children.get (1);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (id);
	if (dims != null)
	    sb.append (dims);
	return sb.toString ();
    }
}
