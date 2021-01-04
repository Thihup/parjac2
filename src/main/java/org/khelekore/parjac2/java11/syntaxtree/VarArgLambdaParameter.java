package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;

public class VarArgLambdaParameter extends LambdaParameter {
    private final VariableArityParameter vap;

    public VarArgLambdaParameter (ParsePosition pos, VariableArityParameter vap) {
	super (pos);
	this.vap = vap;
    }

    @Override public Object getValue () {
	return vap;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (vap);
    }
}
