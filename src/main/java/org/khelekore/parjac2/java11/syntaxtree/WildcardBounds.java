package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class WildcardBounds extends SyntaxTreeNode {
    private Token type;
    private ParseTreeNode referenceType;

    public WildcardBounds (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	type = ((TokenNode)children.get (0)).getToken ();
	referenceType = children.get (1);
    }

    @Override public Object getValue() {
	return type.getName () + " " + referenceType;
    }
}
