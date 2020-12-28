package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ExportsDirective extends PackageNameTo<PackageName, ModuleName> {
    public ExportsDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (r, n, children);
    }

    @Override protected String getType () {
	return "exports";
    }

    @Override protected String getThing () {
	return "to";
    }
}
