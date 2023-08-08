package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LocalVariableDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final VariableDeclaratorList list;

    public LocalVariableDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.size () > 2) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	}  else{
	    modifiers = List.of ();
	}
	type = children.get (i++);
	list = (VariableDeclaratorList)children.get (i);
    }

    public LocalVariableDeclaration (List<ParseTreeNode> modifiers, ParseTreeNode type, VariableDeclaratorList list) {
	super (!modifiers.isEmpty () ? modifiers.get (0).getPosition () : type.getPosition ());
	this.modifiers = modifiers;
	this.type = type;
	this.list = list;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (list);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	v.accept (list);
    }

    public List<ParseTreeNode> getModifiers () {
	return modifiers;
    }

    public ParseTreeNode getType () {
	return type;
    }
}
