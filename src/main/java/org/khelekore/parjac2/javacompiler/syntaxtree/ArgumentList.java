package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ArgumentList extends CommaListBase {
    public ArgumentList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (rule, n, children);
    }

    public ArgumentList (ParsePosition pos, List<ParseTreeNode> children) {
	super (pos, children);
    }
}
