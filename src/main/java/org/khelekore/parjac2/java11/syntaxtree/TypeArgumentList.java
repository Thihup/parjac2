package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class TypeArgumentList extends CommaListBase {
    public TypeArgumentList (Path path, Rule r, ParseTreeNode tal, List<ParseTreeNode> children) {
	super (path, r, tal, children);
    }
}
