package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LocalVariableDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final VariableDeclaratorList list;

    public LocalVariableDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	if (rule.size () > 2) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	}  else{
	    modifiers = List.of ();
	}
	type = children.get (i++);
	list = (VariableDeclaratorList)children.get (i);
	list.getDeclarators ().forEach (vd -> vd.type (type));
    }

    public LocalVariableDeclaration (ParsePosition pos, ParseTreeNode type, String name, ParseTreeNode initializer) {
	super (pos);
	modifiers = List.of ();
	this.type = type;
	list = new VariableDeclaratorList (pos, name, initializer);
	list.getDeclarators ().forEach (vd -> vd.type (type));
    }

    @Override public Object getValue () {
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

    public List<VariableDeclarator> getDeclarators () {
	return list.getDeclarators ();
    }
}
