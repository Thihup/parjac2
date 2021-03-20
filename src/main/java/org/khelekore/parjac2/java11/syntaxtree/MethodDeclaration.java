package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.java11.Flags;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodDeclaration extends SyntaxTreeNode {
    private List<ParseTreeNode> modifiers;
    private MethodHeader header;
    private ParseTreeNode body; // Block or ;
    private int flags;

    private static FlagCalculator flagCalculator = new FlagCalculator (0);
    static {
	flagCalculator.addInvalid (Flags.ACC_PUBLIC | Flags.ACC_PROTECTED | Flags.ACC_PRIVATE);
	flagCalculator.addInvalidIf (Flags.ACC_ABSTRACT,
				     Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_FINAL |
				     Flags.ACC_NATIVE | Flags.ACC_STRICT | Flags.ACC_SYNCHRONIZED);
	flagCalculator.addInvalid (Flags.ACC_NATIVE | Flags.ACC_STRICT);
    }

    public MethodDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	modifiers = (rule.size () > 2) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	header = (MethodHeader)children.get (i++);
	body = children.get (i);
	flags = flagCalculator.calculate (ctx, modifiers, getPosition ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (header).append (" ").append (body);
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

    public String getName () {
	return header.getName ();
    }
}
