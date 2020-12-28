package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class DoStatement extends SyntaxTreeNode {
    private final ParseTreeNode statement;
    private final ParseTreeNode expression;

    public DoStatement (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	statement = children.get (1);
	expression = children.get (4);
    }

    @Override public Object getValue () {
	return "do " + statement + " while (" + expression + ");";
    }
}
