package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Throws extends SyntaxTreeNode {
    private ExceptionTypeList exceptionTypeList;

    public Throws (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	exceptionTypeList = (ExceptionTypeList)children.get (1);
    }

    @Override public Object getValue() {
	return "throws " + exceptionTypeList;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (exceptionTypeList);
    }

    public ExceptionTypeList getExceptions () {
	return exceptionTypeList;
    }

    public List<ClassType> thrownTypes () {
	return exceptionTypeList.get ();
    }
}
