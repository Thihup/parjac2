package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;
import java.util.function.Consumer;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Multiple extends SyntaxTreeNode {
    private final int ruleGroupId; // zero or more of this group
    private final String name;
    private final List<ParseTreeNode> nodes;

    public Multiple (ParsePosition pos, int ruleGroupId, String name, List<ParseTreeNode> nodes) {
	super (pos);
	this.ruleGroupId = ruleGroupId;
	this.name = name;
	this.nodes = nodes;
    }

    public int getInternalGroupId () {
	return ruleGroupId;
    }

    @Override public Object getValue () {
	return name;
    }

    @SuppressWarnings("unchecked") public <T extends ParseTreeNode> List<T> get () {
	return (List<T>)nodes;
    }

    @Override public List<ParseTreeNode> getChildren () {
	return nodes;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	nodes.forEach (v::accept);
    }

    public void addAll (List<ParseTreeNode> newNodes) {
	nodes.addAll (newNodes);
    }

    public ParseTreeNode get (int i) {
	return nodes.get (i);
    }

    public int size () {
	return nodes.size ();
    }

    public void forEach (Consumer<? super ParseTreeNode> c) {
	nodes.forEach (c);
    }
}
