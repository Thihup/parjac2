package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeArgumentList extends CommaListBase {
    public TypeArgumentList (Rule r, ParseTreeNode tal, List<ParseTreeNode> children) {
	super (r, tal, children);
    }
}
