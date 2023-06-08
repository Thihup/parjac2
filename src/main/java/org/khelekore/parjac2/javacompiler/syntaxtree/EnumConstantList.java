package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumConstantList extends SyntaxTreeNode {
    private List<EnumConstant> constants;
    public EnumConstantList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () == 1) {
	    constants = List.of ((EnumConstant)children.get (0));
	} else {
	    constants = new ArrayList<> ();
	    constants.add ((EnumConstant)children.get (0));
	    Multiple z = (Multiple)children.get (1);
	    for (int j = 1, e = z.size (); j < e; j += 2)
		constants.add ((EnumConstant)z.get (j));
	}
    }

    @Override public Object getValue () {
	return constants.stream ().map (ec -> ec.toString ()).collect (Collectors.joining (", "));
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	constants.forEach (v::accept);
    }

    public List<EnumConstant> getConstants () {
	return constants;
    }
}
