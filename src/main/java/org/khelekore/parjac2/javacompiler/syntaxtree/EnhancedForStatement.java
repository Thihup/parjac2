package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnhancedForStatement extends SyntaxTreeNode {
    private final LocalVariableDeclaration lv;
    private final ParseTreeNode expression;
    private final ParseTreeNode statement;

    public EnhancedForStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 2;
	lv = (LocalVariableDeclaration)children.get (i++);
	i++; // :
	expression = children.get (i++);
	i++;
	statement = children.get (i);
    }

    public LocalVariableDeclaration localVariable () {
	return lv;
    }

    public ParseTreeNode expression () {
	return expression;
    }

    public ParseTreeNode statement () {
	return statement;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("for (").append (lv.getValue ()).append (" : ")
	    .append (expression).append (")").append (statement);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (lv);
	v.accept (expression);
	v.accept (statement);
    }
}
