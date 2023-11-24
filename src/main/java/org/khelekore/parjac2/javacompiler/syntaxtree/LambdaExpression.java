package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.MethodInfo;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LambdaExpression extends SyntaxTreeNode {
    private final LambdaParameters params;
    private final ParseTreeNode body;
    private FullNameHandler type;
    private MethodInfo mi;

    public LambdaExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	params = (LambdaParameters)children.get (0);
	body = children.get (2);
    }

    @Override public Object getValue () {
	return params + " -> " + body;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (params);
	v.accept (body);
    }

    public LambdaParameters params () {
	return params;
    }

    public ParseTreeNode body () {
	return body;
    }

    public FullNameHandler type () {
	return type;
    }

    public MethodInfo methodInfo () {
	return mi;
    }

    public void type (FullNameHandler type, MethodInfo mi) {
	this.type = type;
	this.mi = mi;
    }

    public int numberOfArguments () {
	return params.numberOfArguments ();
    }

    public String parameterName (int i) {
	return params.parameterName (i);
    }

    public FullNameHandler parameter (int i) {
	return mi.parameter (i);
    }

    public FullNameHandler result () {
	return mi.result ();
    }

    public FullNameHandler lambdaParameter (int i) {
	return params.parameter (i);
    }

    public FullNameHandler lambdaResult () {
	return FullNameHelper.type (body);
    }
}
