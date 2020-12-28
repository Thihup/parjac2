package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class BasicForStatement extends SyntaxTreeNode {
    private final ParseTreeNode forInit;
    private final ParseTreeNode expression;
    private final ParseTreeNode forUpdate;
    private final ParseTreeNode statement;

    public BasicForStatement (Path path, Grammar grammar, Rule rule,
			      ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	// for' '(' [ForInit] ';' [Expression] ';' [ForUpdate] ')' Statement
	int i = 2;
	forInit = grammar.isRule (rule.get (i)) ? children.get (i++) : null;
	i++;  // ';'
	expression = grammar.isRule (rule.get (i)) ? children.get (i++) : null;
	i++;  // ';'
	forUpdate = grammar.isRule (rule.get (i)) ? children.get (i++) : null;
	i++;  // ';'
	statement = children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("for (");
	if (forInit != null)
	    sb.append (forInit);
	sb.append ("; ");
	if (expression != null)
	    sb.append (expression);
	sb.append ("; ");
	if (forUpdate != null)
	    sb.append (forUpdate);
	sb.append (") ").append (statement);
	return sb.toString ();
    }
}
