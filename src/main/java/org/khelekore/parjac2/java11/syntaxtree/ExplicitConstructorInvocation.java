package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ExplicitConstructorInvocation extends SyntaxTreeNode {
    private final ParseTreeNode type; // ExpresisonName or Primary
    private final TypeArguments types;
    private final Token where; // this or super
    private final ArgumentList argumentList;
    public ExplicitConstructorInvocation (Path path, Grammar grammar, Rule rule,
					  ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.get (i) == grammar.getRuleGroupId ("ExpressionName") ||
	    rule.get (i) == grammar.getRuleGroupId ("Primary")) {
	    type = children.get (i);
	    i += 2;
	} else {
	    type = null;
	}
	if (rule.get (i) == grammar.getRuleGroupId ("TypeArguments"))
	    types = (TypeArguments)children.get (i++);
	else
	    types = null;
	where = ((TokenNode)children.get (i++)).getToken ();
	i++; // (
	if (rule.get (i) == grammar.getRuleGroupId ("ArgumentList"))
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
}
