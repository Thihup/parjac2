package org.khelekore.parjac2.parsetree;

import org.khelekore.parjac2.parser.Rule;
import java.util.List;

public class RuleSyntaxTreeNode implements SyntaxTreeNode {
    private final Rule rule;
    private final List<SyntaxTreeNode> children;

    public RuleSyntaxTreeNode (Rule rule, List<SyntaxTreeNode> children) {
	this.rule = rule;
	this.children = children;
    }

    @Override public String getId () {
	return rule.getName ();
    }

    @Override public Object getValue () {
	return null;
    }

    @Override public boolean isRule () {
	return true;
    }

    @Override public boolean isToken () {
	return false;
    }

    @Override public List<SyntaxTreeNode> getChildren () {
	return children;
    }

    public Rule getRule () {
	return rule;
    }

    @Override public String toString () {
	return rule + ":"  + children;
    }
}