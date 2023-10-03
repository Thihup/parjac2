package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public final class SingleTypeImportDeclaration extends ImportDeclaration {
    private TypeName typename;
    public SingleTypeImportDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	this.typename = (TypeName)children.get (1);
    }

    @Override public Object getValue () {
	return "import " + typename + ";";
    }

    public TypeName getName () {
	return typename;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (typename);
    }
}
