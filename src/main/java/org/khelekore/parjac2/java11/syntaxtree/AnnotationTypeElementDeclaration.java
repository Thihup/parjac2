package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class AnnotationTypeElementDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final String id;
    private final Dims dims;
    private final DefaultValue defaultValue;

    public AnnotationTypeElementDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = List.of ();
	}
	type = children.get (i++);
	id = ((Identifier)children.get (i++)).getValue ();
	i += 2;
	dims = (children.get (i) instanceof Dims) ? (Dims)children.get (i++) : null;
	defaultValue = (children.get (i) instanceof DefaultValue) ? (DefaultValue)children.get (i++) : null;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (id).append (" ()");
	if (dims != null)
	    sb.append (dims);
	if (defaultValue != null)
	    sb.append (defaultValue);
	sb.append (";");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	if (dims != null)
	    v.accept (dims);
	if (defaultValue != null)
	    v.accept (defaultValue);
    }
}
