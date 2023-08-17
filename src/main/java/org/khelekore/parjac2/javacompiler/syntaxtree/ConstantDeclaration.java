package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.objectweb.asm.Opcodes;

public class ConstantDeclaration extends FlaggedBase {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final VariableDeclaratorList variables;

    private static FlagCalculator flagCalculator =
	new FlagCalculator (Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC);

    public ConstantDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.size () > 3) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	type = children.get (i++);
	variables = (VariableDeclaratorList)children.get (i++);
	flags = flagCalculator.calculate (ctx, modifiers, n.getPosition ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (variables).append (";");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	v.accept (variables);
    }
}
