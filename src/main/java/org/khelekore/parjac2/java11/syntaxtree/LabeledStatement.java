package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LabeledStatement extends SyntaxTreeNode {
    private String id;
    private ParseTreeNode statement;
    public LabeledStatement (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	id = ((Identifier)children.get (0)).getValue ();
	statement = children.get (2);
    }

    @Override public Object getValue() {
	return id + ":" + statement;
    }
}
