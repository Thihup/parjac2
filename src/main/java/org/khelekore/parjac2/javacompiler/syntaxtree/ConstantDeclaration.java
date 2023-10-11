package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstantDeclaration extends FieldDeclarationBase {
    private static FlagCalculator flagCalculator =
	new FlagCalculator (Flags.ACC_PUBLIC + Flags.ACC_FINAL + Flags.ACC_STATIC);

    public ConstantDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (ctx, rule, n, children, flagCalculator);
    }
}
