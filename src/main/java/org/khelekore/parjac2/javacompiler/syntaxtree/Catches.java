package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Catches extends ListBase {
    public Catches (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (rule, n, children, 0, 1);
    }
}
