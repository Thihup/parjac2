package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodDeclaration extends MethodDeclarationBase {
    private static FlagCalculator flagCalculator = new FlagCalculator (0);
    static {
	flagCalculator.addInvalid (Flags.ACC_PUBLIC | Flags.ACC_PROTECTED | Flags.ACC_PRIVATE);
	flagCalculator.addInvalidIf (Flags.ACC_ABSTRACT,
				     Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_FINAL |
				     Flags.ACC_NATIVE | Flags.ACC_STRICT | Flags.ACC_SYNCHRONIZED);
	flagCalculator.addInvalid (Flags.ACC_NATIVE | Flags.ACC_STRICT);
    }

    public MethodDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (ctx, rule, n, children, flagCalculator);
    }

    public MethodDeclaration (ParsePosition pos, int flags, String name, ParseTreeNode result, Block body) {
	super (pos, flags, name, result, body);
    }
}
