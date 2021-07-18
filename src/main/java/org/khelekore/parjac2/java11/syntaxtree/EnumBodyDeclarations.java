package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumBodyDeclarations extends ClassBody {
    public EnumBodyDeclarations (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (ctx, rule, n, children);
    }

    @Override protected boolean hasDeclarations (Rule rule) {
	return rule.size () > 1;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (";");
	declarations.forEach (d -> sb.append (d).append ("\n"));
	return sb.toString ();
    }
}
