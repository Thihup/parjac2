package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class ListBase extends SyntaxTreeNode {
    private final List<ParseTreeNode> params;

    public ListBase (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children,
		     int start, int every) {
	super (n.getPosition ());
	params = new ArrayList<> ();
	params.add (children.get (0));
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int i = start, e = z.size (); i < e; i += every)
		params.add (z.get (i));
	}
    }

    public List<ParseTreeNode> get () {
	return params;
    }

    @Override public Object getValue () {
	return params.toString ();
    }
}
