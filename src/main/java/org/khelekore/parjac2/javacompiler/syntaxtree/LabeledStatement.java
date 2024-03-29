package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LabeledStatement extends SyntaxTreeNode {
    private final String id;
    private final ParseTreeNode statement;

    public LabeledStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	id = ((Identifier)children.get (0)).getValue ();
	statement = children.get (2);
    }

    public String id () {
	return id;
    }

    public ParseTreeNode statement () {
	return statement;
    }

    @Override public Object getValue () {
	return id + ":" + statement;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (statement);
    }
}
