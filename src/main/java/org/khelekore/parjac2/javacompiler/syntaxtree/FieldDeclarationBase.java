package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class FieldDeclarationBase extends FlaggedBase {
    private List<ParseTreeNode> modifiers;
    private ParseTreeNode type;  // may be ClassType or TokenType (for char and similar)
    private VariableDeclaratorList list;

    public FieldDeclarationBase (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children, FlagCalculator flagCalculator) {
	super (n.position ());
	int i = 0;
	modifiers = (rule.size () > 3) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	type = children.get (i++);
	list = (VariableDeclaratorList)children.get (i++);
	flags = flagCalculator.calculate (ctx, modifiers, n.position ());
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (list).append (";");
	return sb.toString ();
    }

    @Override public void visitChildNodes(NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	v.accept (list);
    }

    public ParseTreeNode getType () {
	return type;
    }

    public List<VariableDeclarator> getVariableDeclarators () {
	return list.getDeclarators ();
    }
}
