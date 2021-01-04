package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ExplicitConstructorInvocation extends SyntaxTreeNode {
    private final ParseTreeNode type; // ExpresisonName or Primary
    private final TypeArguments types;
    private final Token where; // this or super
    private final ArgumentList argumentList;

    public ExplicitConstructorInvocation (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("ExpressionName") ||
	    rule.get (i) == ctx.getGrammar ().getRuleGroupId ("Primary")) {
	    type = children.get (i);
	    i += 2;
	} else {
	    type = null;
	}
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("TypeArguments"))
	    types = (TypeArguments)children.get (i++);
	else
	    types = null;
	where = ((TokenNode)children.get (i++)).getToken ();
	i++; // (
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("ArgumentList"))
	    argumentList = (ArgumentList)children.get (i);
	else
	    argumentList = null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (type != null)
	    sb.append (type).append (".");
	if (types != null)
	    sb.append (types);
	sb.append (where);
	sb.append ("(");
	if (argumentList != null)
	    sb.append (argumentList);
	sb.append (");");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (type != null)
	    v.accept (type);
	if (types != null)
	    v.accept (types);
	if (argumentList != null)
	    v.accept (argumentList);
    }
}
