package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class InterfaceMethodDeclaration extends MethodDeclarationBase {
    private static FlagCalculator flagCalculator = new FlagCalculator (0);
    static {
	flagCalculator.addDefaultUnless (Flags.ACC_PUBLIC, Flags.ACC_PRIVATE);
	flagCalculator.addDefaultUnless (Flags.ACC_ABSTRACT,
					 Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_DEFAULT);

	flagCalculator.addInvalid (Flags.ACC_PUBLIC | Flags.ACC_PRIVATE);
	flagCalculator.addInvalid (Flags.ACC_ABSTRACT | Flags.ACC_DEFAULT | Flags.ACC_STATIC);
	flagCalculator.addInvalid (Flags.ACC_PRIVATE | Flags.ACC_ABSTRACT | Flags.ACC_DEFAULT);
	flagCalculator.addInvalid (Flags.ACC_ABSTRACT | Flags.ACC_STRICT);
    }

    public InterfaceMethodDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (ctx, rule, n, children, flagCalculator);
	int clash = flags & (Flags.ACC_DEFAULT | Flags.ACC_PRIVATE | Flags.ACC_STATIC);
	if (clash > 0 &&
	    !(body instanceof Block))
	    ctx.error (position (), "Method marked as %s requires a body",
		       ctx.getTokenNameString (clash));
	if ((flags & Flags.ACC_ABSTRACT) > 0 && body instanceof Block)
	    ctx.error (position (), "Abstract method may not have a body");
    }
}
