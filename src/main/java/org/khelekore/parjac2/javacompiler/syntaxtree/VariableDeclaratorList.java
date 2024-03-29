package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class VariableDeclaratorList extends SyntaxTreeNode {
    private final List<VariableDeclarator> declarators;

    public VariableDeclaratorList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	declarators = new ArrayList<> ();
	declarators.add ((VariableDeclarator)children.get (0));
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1, e = z.size (); i < e; i += 2)
		declarators.add ((VariableDeclarator)z.get (i));
	}
    }

    public VariableDeclaratorList (VariableDeclarator v) {
	super (v.position ());
	declarators = List.of (v);
    }

    public VariableDeclaratorList (ParsePosition pos, String name, ParseTreeNode initializer) {
	super (pos);
	declarators = List.of (new VariableDeclarator (pos, name, initializer));
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (declarators.get (0));
	for (int i = 1; i < declarators.size (); i++)
	    sb.append (", ").append (declarators.get (i));
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	declarators.forEach (v::accept);
    }

    public List<VariableDeclarator> getDeclarators () {
	return declarators;
    }
}
