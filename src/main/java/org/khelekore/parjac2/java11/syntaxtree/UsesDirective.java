package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class UsesDirective extends ModuleDirective {
    private TypeName typeName;

    public UsesDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	typeName = (TypeName)children.get (1);
    }

    @Override public Object getValue() {
	return "uses " + typeName + ";";
    }
}
