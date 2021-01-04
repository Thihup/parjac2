package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TryStatement extends SyntaxTreeNode {
    private final ResourceList resources;
    private final Block block;
    private final Catches catches;
    private final Finally finallyBlock;

    public TryStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 1;
	if (children.get (i) instanceof ResourceSpecification) {
	    ResourceSpecification rs = (ResourceSpecification)children.get (i++);
	    resources = rs.getResources ();
	} else {
	    resources = null;
	}
	block = (Block)children.get (i++);
	if (rule.size () >  i && children.get (i) instanceof Catches) {
	    catches = (Catches)children.get (i++);
	} else {
	    catches = null;
	}
	if (rule.size () >  i && children.get (i) instanceof Finally) {
	    finallyBlock = (Finally)children.get (i);
	} else {
	    finallyBlock = null;
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("try");
	if (resources != null)
	    sb.append ("(").append (resources).append (")");
	sb.append (" ").append (block);
	if (catches != null)
	    sb.append (catches);
	if (finallyBlock != null)
	    sb.append (finallyBlock);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (resources != null)
	    v.accept (resources);
	v.accept (block);
	if (catches != null)
	    v.accept (catches);
	if (finallyBlock != null)
	    v.accept (finallyBlock);
    }
}
