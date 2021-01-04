package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ResourceSpecification extends SyntaxTreeNode {
    private final ResourceList resources;
    public ResourceSpecification (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	resources = (ResourceList)children.get (1);
    }

    @Override public Object getValue () {
	return "(" + resources + ")";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (resources);
    }

    public ResourceList getResources () {
	return resources;
    }
}
