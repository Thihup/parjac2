package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ReceiverParameter extends SyntaxTreeNode {
    private List<Annotation> annotations;
    private ParseTreeNode type;
    private String id;

    public ReceiverParameter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    annotations = ((Multiple)children.get (i++)).get ();
	else
	    annotations = Collections.emptyList ();
	type = children.get (i++);

	// {Annotation} UnannType ['Identifier' '.'] 'this'
	if (rule.size () > i + 1)
	    id = ((Identifier)children.get (i)).getValue ();
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append (type).append (" ");
	if (id != null)
	    sb.append (id).append (".");
	sb.append ("this");
	return sb.toString ();
    }
}
