package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableArityParameter extends FormalParameterBase {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final List<Annotation> annotations;
    private final String id;

    public VariableArityParameter (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	modifiers = (children.get (i) instanceof Multiple) ?
	    ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	type = children.get (i++);
	annotations = (children.get (i) instanceof Multiple) ?
	    ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	i++; // ...
	id = ((Identifier)children.get (i)).getValue ();
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ");
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append ("...").append (id);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	annotations.forEach (v::accept);
    }
}
