package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.objectweb.asm.Opcodes;

public class InterfaceMethodDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final MethodHeader header;
    private final ParseTreeNode body;
    private int flags;

    private static FlagCalculator flagCalculator = new FlagCalculator (0);
    static {
	flagCalculator.addDefaultUnless (Opcodes.ACC_PUBLIC, Opcodes.ACC_PRIVATE);
	flagCalculator.addDefaultUnless (Opcodes.ACC_ABSTRACT,
					 Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC); // |Opcodes.DEFAULT);

	flagCalculator.addInvalid (Opcodes.ACC_PUBLIC | Opcodes.ACC_PRIVATE);
	flagCalculator.addInvalid (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STATIC); // | Opcodes.ACC_DEFAULT);
	flagCalculator.addInvalid (Opcodes.ACC_PRIVATE | Opcodes.ACC_ABSTRACT);
	flagCalculator.addInvalid (Opcodes.ACC_ABSTRACT | Opcodes.ACC_STRICT);
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
	flags = flagCalculator.calculate (ctx, modifiers, header.getPosition ());
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
