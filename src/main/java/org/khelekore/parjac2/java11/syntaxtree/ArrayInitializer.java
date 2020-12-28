package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayInitializer extends SyntaxTreeNode {
    private final  VariableInitializerList variableList;
    public ArrayInitializer (Path path, Grammar grammar, Rule rule,
			     ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.get (1) == grammar.getRuleGroupId ("VariableInitializerList"))
	    variableList = (VariableInitializerList)children.get (1);
	else
	    variableList = null;
    }
    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	if (variableList != null)
	    sb.append (variableList);
	sb.append ("}");
	return sb.toString ();
    }
}
