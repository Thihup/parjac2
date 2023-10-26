package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Block extends SyntaxTreeNode {
    private final BlockStatements statements;

    public Block (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	statements = (rule.size () > 2) ? (BlockStatements)children.get (1) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{\n");
	if (statements != null)
	    sb.append (statements);
	sb.append ("}");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (statements != null)
	    v.accept (statements);
    }

    public BlockStatements getStatements () {
	return statements;
    }

    public List<ParseTreeNode> get () {
	if (statements == null)
	    return List.of ();
	return statements.getStatements ();
    }
}
