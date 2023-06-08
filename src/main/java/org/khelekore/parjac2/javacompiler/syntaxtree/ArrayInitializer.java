package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayInitializer extends SyntaxTreeNode {
    private final  VariableInitializerList variableList;
    public ArrayInitializer (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.get (1) == ctx.getGrammar ().getRuleGroupId ("VariableInitializerList"))
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

    @Override public void visitChildNodes (NodeVisitor v) {
	if (variableList != null)
	    v.accept (variableList);
    }
}
