package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;
import org.objectweb.asm.Opcodes;

public class ConstantDeclaration extends SyntaxTreeNode {
    private final List<ParseTreeNode> modifiers;
    private final ParseTreeNode type;
    private final VariableDeclaratorList variables;

    private int flags;

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
	setFlags (ctx);
    }

    private void setFlags (Context ctx) {
	flags = 0;
	for (ParseTreeNode m : modifiers) {
	    int modifierFlag = 0;
	    if (m instanceof TokenNode) {
		TokenNode tn = (TokenNode)m;
		Token t = tn.getToken ();
		if (t == ctx.getTokens ().PUBLIC)
		    modifierFlag = Opcodes.ACC_PUBLIC;
		else if (t == ctx.getTokens ().FINAL)
		    modifierFlag = Opcodes.ACC_FINAL;
		else if (t == ctx.getTokens ().STATIC)
		    modifierFlag = Opcodes.ACC_STATIC;
		else
		    ctx.error (tn.getPosition (), "Unknown token: %s", t.getName ());
		if ((flags & modifierFlag) != 0)
		    ctx.error (tn.getPosition (), "Duplicate modifier %s found", t.getName ());
		flags |= modifierFlag;
	    }
	}
	flags |= Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_STATIC;
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

    public int getFlags () {
	return flags;
    }
}
