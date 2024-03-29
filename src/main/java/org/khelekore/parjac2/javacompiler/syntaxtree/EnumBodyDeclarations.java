package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumBodyDeclarations extends ClassBody {
    public EnumBodyDeclarations (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (ctx, rule, n, children);
    }

    public EnumBodyDeclarations (ParsePosition pos) {
	super (pos);
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
