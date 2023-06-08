package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class OpensDirective extends PackageNameTo<PackageName, ModuleName> {
    public OpensDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (r, n, children);
    }

    @Override protected String getType () {
	return "opens";
    }

    @Override protected String getThing () {
	return "to";
    }
}
