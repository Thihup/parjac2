package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Throws extends SyntaxTreeNode {
    private ExceptionTypeList exceptionTypeList;

    public Throws (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	exceptionTypeList = (ExceptionTypeList)children.get (1);
    }

    @Override public Object getValue() {
	return "throws " + exceptionTypeList;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (exceptionTypeList);
    }
}
