package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ClassBody extends SyntaxTreeNode {
    private List<ParseTreeNode> declarations;
    public ClassBody (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	declarations = (rule.size () > 2) ? ((Multiple)children.get (1)).get () : Collections.emptyList ();
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (" {\n");
	for (ParseTreeNode d : declarations)
	    sb.append (d).append ("\n");
	sb.append ("}\n");
	return sb.toString ();
    }
}
