package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchLabels extends SyntaxTreeNode {
    private final List<SwitchLabelColon> labels = new ArrayList<> ();

    public SwitchLabels (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (children.size () > 1) {
	    Multiple m = (Multiple)children.get (0);
	    labels.addAll (m.get ());
	    labels.add ((SwitchLabelColon)children.get (1));
	} else {
	    labels.add ((SwitchLabelColon)children.get (0));
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	for (SwitchLabelColon l : labels)
	    sb.append (l.getValue ());
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	labels.forEach (v::accept);
    }

    public List<SwitchLabelColon> getLabels () {
	return labels;
    }
}
