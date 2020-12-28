package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ExceptionTypeList extends SyntaxTreeNode {
    private List<ClassType> types;
    public ExceptionTypeList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	types = new ArrayList<> ();
	types.add ((ClassType)children.get (0));
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1, e = z.size (); i < e; i += 2)
		types.add ((ClassType)z.get (i));
	}
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (types.get (0));
	for (int i = 1; i < types.size (); i++)
	    sb.append (", ").append (types.get (i));
	return sb.toString ();
    }
}
