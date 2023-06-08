package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class InterfaceMethodDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final MethodHeader header;
    private final ParseTreeNode body; // either ';' or a Block.
    private int flags;

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
	super (n.getPosition ());
	int i = 0;
	if (rule.size () > 2){
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	header = (MethodHeader)children.get (i++);
	body = children.get (i);
	flags = flagCalculator.calculate (ctx, modifiers, getPosition ());
	int clash = flags & (Flags.ACC_DEFAULT | Flags.ACC_PRIVATE | Flags.ACC_STATIC);
	if (clash > 0 &&
	    !(body instanceof Block))
	    ctx.error (getPosition (), "Method marked as %s requires a body",
		       ctx.getTokenNameString (clash));
	if ((flags & Flags.ACC_ABSTRACT) > 0 && body instanceof Block)
	    ctx.error (getPosition (), "Abstract method may not have a body");

    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (header).append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (header);
	v.accept (body);
    }

    public int getFlags () {
	return flags;
    }
}
