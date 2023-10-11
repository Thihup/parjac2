package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FieldDeclaration extends FieldDeclarationBase {

    private static FlagCalculator flagCalculator = FlagCalculator.SIMPLE_ACCESS;

    public FieldDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (ctx, rule, n, children, flagCalculator);
    }
}
