package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.java11.Flags;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class FieldDeclaration extends SyntaxTreeNode {
    private List<ParseTreeNode> modifiers;
    private ParseTreeNode type;
    private VariableDeclaratorList list;
    private int flags;

    private static FlagCalculator flagCalculator = new FlagCalculator (0);

    static {
	flagCalculator.addInvalid (Flags.ACC_PUBLIC | Flags.ACC_PROTECTED | Flags.ACC_PRIVATE);
    }

    public FieldDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	modifiers = (rule.size () > 3) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	type = children.get (i++);
	list = (VariableDeclaratorList)children.get (i++);
	flags = flagCalculator.calculate (ctx, modifiers, n.getPosition ());
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (type).append (" ").append (list).append (";");
	return sb.toString ();
    }

    @Override public void visitChildNodes(NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (type);
	v.accept (list);
    }

    public int getFlags () {
	return flags;
    }
}
