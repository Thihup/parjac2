package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class StatementExpressionList extends CommaListBase {
    public StatementExpressionList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (path, rule, n, children);
    }
}
