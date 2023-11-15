package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class PrimitiveType extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final Token type;
    private FullNameHandler.Primitive fullName;

    public PrimitiveType (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	if (rule.size () > 1) {
	    annotations = ((Multiple)children.get (0)).get ();
	    type = ((TokenNode)children.get (1)).token ();
	} else {
	    annotations = Collections.emptyList ();
	    type = ((TokenNode)children.get (0)).token ();
	}
    }

    public Token type () {
	return type;
    }

    public void fullName (FullNameHandler.Primitive fullName) {
	this.fullName = fullName;
    }

    public FullNameHandler.Primitive fullName () {
	return fullName;
    }

    public boolean hasFullName () {
	return fullName != null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	sb.append (type);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (v::accept);
    }
}
