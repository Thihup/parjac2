package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class CaseLabel extends SwitchLabel {
    private final List<ParseTreeNode> expressions;

    public CaseLabel (Context ctx, ParsePosition pos, List<ParseTreeNode> children) {
	super (pos);
	if (children.size () == 2) {
	    expressions = List.of (children.get (1));
	} else {
	    expressions = new ArrayList<> ();
	    expressions.add (children.get (1));
	    Multiple z = (Multiple)children.get (2);
	    for (int i = 1; i < z.size (); i += 2)
		expressions.add (z.get (i));
	}

	int nullFound = 0;
	int defaultFound = 0;
	int otherFound = 0;
	/* Grammar in chapter 19 say this:
	  SwitchLabel:
	    case CaseConstant {, CaseConstant}
	    case null [, default]
	    case CasePattern [Guard]
	    default
	*/
	for (ParseTreeNode p : expressions) {
	    if (p instanceof TokenNode tn) {
		if (tn.token () == ctx.getTokens ().DEFAULT) {
		    if (defaultFound > 0 || otherFound > 0 || nullFound != 1) {
			ctx.error (p.position (), "default not allowed here");
		    }
		    defaultFound++;
		} else if (tn.token () == ctx.getTokens ().NULL) {
		    if (nullFound > 0 || otherFound > 0 || defaultFound > 0)
			ctx.error (p.position (), "null not allowed here");
		    nullFound++;
		} else {
		    if (nullFound > 0)
			ctx.error (p.position (), "CaseConstant can not follow null");
		    else if (defaultFound > 0)
			ctx.error (p.position (), "CaseConstant can not follow default");
		    otherFound++;
		}
 	    }
	}
    }

    @Override public Object getValue () {
	String labels = expressions.stream ().map (e -> e.getValue ().toString ()).collect (Collectors.joining (", "));
	return "case " + labels;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	expressions.forEach (v::accept);
    }
}
