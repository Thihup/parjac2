package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ExpressionName extends DottedName {
    public ExpressionName (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (rule, n, children);
    }

    public ExpressionName (ParsePosition pos, String name) {
	super (pos, List.of (name));
    }
}
