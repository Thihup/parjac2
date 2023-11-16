package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableArityParameter extends FormalParameterBase {
    private final List<ParseTreeNode> modifiers;
    private final ArrayType type;
    private final List<Annotation> annotations;
    private final String id;

    public VariableArityParameter (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	modifiers = (children.get (i) instanceof Multiple) ?
	    ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	ParseTreeNode p = children.get (i++);
	if (p instanceof ClassType ct) {
	    type = array (ct);
	} else if (p instanceof PrimitiveType pt) {
	    type = array (pt);
	} else {
	    System.err.println ("unhandled type: " + p);
	    type = null;
	}
	annotations = (children.get (i) instanceof Multiple) ?
	    ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	i++; // ...
	id = ((Identifier)children.get (i)).getValue ();
    }

    private ArrayType array (ClassType ct) {
	return new ArrayType (ct, 1);
    }

    private ArrayType array (PrimitiveType pt) {
	return new ArrayType (pt, 1);
    }

    @Override public Object getValue () {
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

    @Override public List<ParseTreeNode> getModifiers () {
	return modifiers;
    }

    @Override public String name () {
	return id;
    }

    @Override public ArrayType type () {
	return type;
    }
}
