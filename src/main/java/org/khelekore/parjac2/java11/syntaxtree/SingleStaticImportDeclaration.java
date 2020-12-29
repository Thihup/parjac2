package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SingleStaticImportDeclaration extends ImportDeclaration {
    private TypeName typename;
    private String id;
    public SingleStaticImportDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	this.typename = (TypeName)children.get (2);
	this.id = ((Identifier)children.get (4)).getValue ();
    }

    @Override public Object getValue () {
	return "import static " + typename + "." + id + ";";
    }
}
