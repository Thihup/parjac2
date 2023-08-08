package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class RecordComponent extends SyntaxTreeNode {

    private final ParseTreeNode rec;

    public RecordComponent (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());

	int s = rule.size ();
	if (s == 1) {
	    rec = (VariableArityParameter)children.get (0);
	} else {
	    rec = new SimpleRecordComponent (rule, n, children);
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (rec);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (rec);
    }
}
