package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class BlockStatements extends SyntaxTreeNode {
    private List<ParseTreeNode> statements;

    public BlockStatements (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	statements = new ArrayList<> ();
	statements.add (children.get (0));
	if (rule.size () > 1)
	    statements.addAll (((Multiple)children.get (1)).get ());
    }

    public BlockStatements (ParsePosition pos, ParseTreeNode statement) {
	super (pos);
	statements = List.of (statement);
    }

    public BlockStatements (ParsePosition pos, List<ParseTreeNode> statements) {
	super (pos);
	this.statements = statements;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	statements.forEach (s -> sb.append (s).append ("\n"));
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	statements.forEach (v::accept);
    }

    public List<ParseTreeNode> statements () {
	return statements;
    }
}
