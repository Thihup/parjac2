package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodInvocation extends SyntaxTreeNode {
    private ParseTreeNode on;
    private boolean isSuper;
    private TypeArguments types;
    private UntypedMethodInvocation mi;

    public MethodInvocation (ParsePosition pos, ParseTreeNode on, boolean isSuper,
			     TypeArguments types, UntypedMethodInvocation mi) {
	super (pos);
	this.on = on;
	this.types = types;
	this.isSuper = isSuper;
	this.mi = mi;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (on != null)
	    sb.append (on).append (".");
	if (isSuper)
	    sb.append ("super.");
	if (types != null)
	    sb.append (types);
	sb.append (mi);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (on != null)
	    v.accept (on);
	if (types != null)
	    v.accept (types);
	v.accept (mi);
    }

    public ParseTreeNode getOn () {
	return on;
    }

    public boolean isSuper () {
	return isSuper;
    }

    public String getMethodName () {
	return mi.getMethodName ();
    }

    public List<ParseTreeNode> getArguments () {
	return mi.getArguments ();
    }
}
