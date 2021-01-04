package org.khelekore.parjac2.parsetree;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import java.util.List;

public class RuleNode implements ParseTreeNode {
    private final Rule rule;
    private final List<ParseTreeNode> children;

    public RuleNode (Rule rule, List<ParseTreeNode> children) {
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

    @Override public List<ParseTreeNode> getChildren () {
	return children;
    }

    @Override public ParsePosition getPosition () {
	return children.get (0).getPosition ();
    }

    public Rule getRule () {
	return rule;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	children.forEach (v::accept);
    }

    @Override public String toString () {
	return rule + ":"  + children;
    }
}