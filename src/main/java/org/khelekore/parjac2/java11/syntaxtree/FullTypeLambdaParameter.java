package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FullTypeLambdaParameter extends LambdaParameter {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final VariableDeclaratorId vid;

    public FullTypeLambdaParameter (ParsePosition pos, List<ParseTreeNode> modifiers,
				    ParseTreeNode type, VariableDeclaratorId vid) {
	super (pos);
	this.modifiers = modifiers;
	this.type = type;
	this.vid = vid;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (vid);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	v.accept (vid);
    }
}
