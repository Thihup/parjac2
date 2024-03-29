package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FormalParameter extends FormalParameterBase {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final VariableDeclaratorId var;

    // since we have the formalParameter method we do not have to care about VariableArityParameter
    public FormalParameter (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	modifiers = (rule.size () > 2) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	type = children.get (i++);
	var = (VariableDeclaratorId)children.get (i);
    }

    public FormalParameter (ParsePosition pos, List<ParseTreeNode> modifiers, ParseTreeNode type, String id, int dims) {
	super (pos);
	this.modifiers = modifiers;
	this.type = type;
	var = new VariableDeclaratorId (pos, id, dims);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (var);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	v.accept (var);
    }

    @Override public List<ParseTreeNode> getModifiers () {
	return modifiers;
    }

    @Override public String name () {
	return var.name ();
    }

    @Override public ParseTreeNode type () {
	return type;
    }
}
