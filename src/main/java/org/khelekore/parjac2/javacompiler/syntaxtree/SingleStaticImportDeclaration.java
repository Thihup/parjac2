package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public final class SingleStaticImportDeclaration extends ImportDeclaration {
    private TypeName typename;
    private String id;

    public SingleStaticImportDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	this.typename = (TypeName)children.get (2);
	this.id = ((Identifier)children.get (4)).getValue ();
    }

    @Override public Object getValue () {
	return "import static " + typename + "." + id + ";";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (typename);
    }

    public TypeName getType () {
	return typename;
    }

    public String getInnerId () {
	return id;
    }

    public String getFullName () {
	return typename.getDotName () + "." + id;
    }

    public TypeName getName () {
	return typename;
    }
}
