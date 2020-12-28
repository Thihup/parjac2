package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumBodyDeclarations extends SyntaxTreeNode {
    private List<ClassBodyDeclaration> body;
    public EnumBodyDeclarations(Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 1) {
	    Multiple z = (Multiple)children.get (1);
	    body = z.get ();
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (";");
	if (body != null)
	    sb.append (body);
	return sb.toString ();
    }
}
