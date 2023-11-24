package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ListOfLambdaParameters extends LambdaParameters {
    private final LambdaParameterList<?> params;

    public ListOfLambdaParameters (ParseTreeNode n, LambdaParameterList<?> params) {
	super (n.position ());
	this.params = params;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("(");
	if (params != null)
	    sb.append (params);
	sb.append (")");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (params != null)
	    v.accept (params);
    }

    @Override public int numberOfArguments () {
	return params == null ? 0 : params.numberOfArguments ();
    }

    @Override public String parameterName (int i) {
	return params.parameterName (i);
    }

    @Override public FullNameHandler parameter (int i) {
	return params.parameter (i);
    }
}
