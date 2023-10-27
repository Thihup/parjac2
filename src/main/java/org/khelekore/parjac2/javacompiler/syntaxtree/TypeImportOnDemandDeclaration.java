package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public final class TypeImportOnDemandDeclaration extends ImportDeclaration {
    private final PackageOrTypeName typename;

    public TypeImportOnDemandDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	this.typename = (PackageOrTypeName)children.get (1);
    }

    @Override public Object getValue () {
	return "import " + typename + ".*;";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (typename);
    }

    public DottedName getName () {
	return typename;
    }
}
