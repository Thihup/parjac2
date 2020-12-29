package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SwitchLabels extends SyntaxTreeNode {
    private List<SwitchLabel> labels;

    public SwitchLabels (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () == 1) {
	    labels = List.of ((SwitchLabel)children.get (0));
	} else {
	    labels = new ArrayList<> ();
	    Multiple z = (Multiple)children.get (0);
	    labels.addAll (z.get ());
	    labels.add ((SwitchLabel)children.get (1));
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	for (SwitchLabel l : labels)
	    sb.append (l.getValue ().toString ()).append ("\n");
	return sb.toString ();
    }

    public List<SwitchLabel> getLabels () {
	return labels;
    }
}
