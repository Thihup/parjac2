package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.java11.Java11Tokens;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumBody extends SyntaxTreeNode {
    private final EnumConstantList constants;
    private final EnumBodyDeclarations declarations;
    public EnumBody (Path path, Java11Tokens java11Tokens, Rule rule,
		     ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 1;
	if (children.get (i) instanceof EnumConstantList) {
	    constants = (EnumConstantList)children.get (i++);
	} else {
	    constants = null;
	}
	if (rule.get (i) == java11Tokens.COMMA.getId ())
	    i++;
	if (children.get (i) instanceof EnumBodyDeclarations) {
	    declarations = (EnumBodyDeclarations)children.get (i);
	} else {
	    declarations = null;
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	if (constants != null)
	    sb.append (constants);
	if (declarations != null)
	    sb.append (declarations);
	sb.append ("}");
	return sb.toString ();
    }
}
