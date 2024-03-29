package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArrayInitializer extends SyntaxTreeNode {
    private final VariableInitializerList variableList;
    private FullNameHandler slotType; // for an int[] this is int

    public ArrayInitializer (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	// icky to know if we have a list or not:
	// '{' [VariableInitializerList] [','] '}'
	if (rule.get (1) == ctx.getGrammar ().getRuleGroupId ("VariableInitializerList"))
	    variableList = (VariableInitializerList)children.get (1);
	else
	    variableList = null;
    }

    public ArrayInitializer (List<ParseTreeNode> parts) {
	super (parts.get (0).position ());
	variableList = new VariableInitializerList (parts);
    }

    public FullNameHandler type () {
	return FullNameHandler.arrayOf (slotType, 1);
    }

    public void slotType (FullNameHandler slotType) {
	this.slotType = slotType;
    }

    public FullNameHandler slotType () {
	return slotType;
    }

    public int size () {
	return variableList != null ? variableList.size () : 0;
    }

    public List<ParseTreeNode> variableInitializers () {
	return variableList != null ? variableList.variableInitializers () : List.of ();
    }

    @Override public Object getValue () {
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
