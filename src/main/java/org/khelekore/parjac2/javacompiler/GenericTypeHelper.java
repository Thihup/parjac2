package org.khelekore.parjac2.javacompiler;

import java.util.Map;

import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeArguments;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.Wildcard;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class GenericTypeHelper {

    private final Map<Token, String> tokenToDescriptor;

    public GenericTypeHelper (Map<Token, String> tokenToDescriptor) {
	this.tokenToDescriptor = tokenToDescriptor;
    }

    public String getGenericType (ParseTreeNode tn) {
	return getGenericType (tn, "");
    }

    public String getGenericType (ParseTreeNode tn, String prefix) {
	if (tn instanceof ClassType ct) {
	    TypeParameter tp = ct.getTypeParameter ();
	    if (tp != null)
		return "T" + tp.getId () + ";";

	    TypeArguments ta = ct.getTypeArguments ();
	    if (ta != null) {
		return "L" + ct.getSlashName () + getTypeArgumentsSignature (ta) + ";";
	    }
	    return "L" + ct.getSlashName () + ";";
	} else if (tn instanceof TokenNode tkn) {
	    Token t = tkn.getToken ();
	    return tokenToDescriptor.get (t);
	}
	// TODO: not sure how we get here.
	//return tn.getExpressionType ().getDescriptor ();
	System.err.println ("Got unhandled type: " + tn.getClass ().getName ());
	return tn.getValue ().toString ();
    }

    private String getTypeArgumentsSignature (TypeArguments ta) {
	if (ta == null)
	    return "";
	StringBuilder sb = new StringBuilder ();
	sb.append ("<");
	for (ParseTreeNode tn : ta.getTypeArguments ()) {
	    switch (tn) {
	    //TODO: we also need to handle type and additional bounds
	    case Wildcard w -> sb.append ("+").append (getGenericType (w.getBounds ().getClassType ()));
	    default -> sb.append (getGenericType (tn));
	    }
	}
	sb.append (">");
	return sb.toString ();
    }
}
