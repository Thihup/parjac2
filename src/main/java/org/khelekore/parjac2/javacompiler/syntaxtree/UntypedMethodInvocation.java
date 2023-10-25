package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class UntypedMethodInvocation extends SyntaxTreeNode {
    private final String methodName;
    private final ArgumentList args;

    public UntypedMethodInvocation (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	methodName = ((Identifier)children.get (0)).getValue ();
	if (rule.size () > 3)
	    args = (ArgumentList)children.get (2);
	else
	    args = null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (methodName).append ("(");
	if (args != null)
	    sb.append (args);
	sb.append (")");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (args != null)
	    v.accept (args);
    }

    public String getMethodName () {
	return methodName;
    }
}
