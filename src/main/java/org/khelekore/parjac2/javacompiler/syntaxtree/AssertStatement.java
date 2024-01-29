package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class AssertStatement extends SyntaxTreeNode {
    private final ParseTreeNode test;
    private final ParseTreeNode errorMessage;

    public AssertStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	test = children.get (1);
	errorMessage = (rule.size () > 3) ? children.get (3) : null;
    }

    public ParseTreeNode test () {
	return test;
    }

    public boolean hasErrorMessage () {
	return errorMessage != null;
    }

    public ParseTreeNode errorMessage () {
	return errorMessage;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("assert ").append (test);
	if (errorMessage != null)
	    sb.append (" : ").append (errorMessage);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (test);
	if (errorMessage != null)
	    v.accept (errorMessage);
    }
}
