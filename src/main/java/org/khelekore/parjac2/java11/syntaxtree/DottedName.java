package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.StringHelper;

public class DottedName extends SyntaxTreeNode {
    private final List<String> nameParts;
    public DottedName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () == 1) {
	    nameParts = new ArrayList<> ();
	    nameParts.add (((Identifier)children.get (0)).getValue ());
	} else {
	    DottedName pot = (DottedName)children.get (0);
	    nameParts = pot.nameParts;
	    nameParts.add (((Identifier)children.get (2)).getValue ());
	}
    }

    @Override public Object getValue() {
	return StringHelper.dotted (nameParts);
    }
}
