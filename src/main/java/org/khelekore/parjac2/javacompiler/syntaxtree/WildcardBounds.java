package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class WildcardBounds extends SyntaxTreeNode {
    private Token type;
    private ParseTreeNode referenceType;

    public WildcardBounds (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	type = ((TokenNode)children.get (0)).token ();
	referenceType = children.get (1);
    }

    @Override public Object getValue() {
	return type.getName () + " " + referenceType;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (referenceType);
    }

    // may be class type or array type
    public ParseTreeNode getClassType () {
	return referenceType;
    }
}
