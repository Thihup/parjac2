package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeArguments;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.Wildcard;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class GenericTypeHelper {

    public static String getGenericType (ParseTreeNode tn) {
	return getGenericType (tn, "");
    }

    public static String getGenericType (ParseTreeNode tn, String prefix) {
	if (tn instanceof ClassType ct) {
	    TypeParameter tp = ct.getTypeParameter ();
	    if (tp != null)
		return "T" + tp.getId () + ";";

	    TypeArguments ta = ct.getTypeArguments ();
	    if (ta != null) {
		return "L" + ct.getSlashName () + getTypeArgumentsSignature (ta) + ";";
	    }
	    return "L" + ct.getSlashName () + ";";
	}
	// TODO: not sure how we get here.
	//return tn.getExpressionType ().getDescriptor ();
	System.err.println ("Got unhandled type: " + tn.getClass ().getName ());
	return tn.getValue ().toString ();
    }

    private static String getTypeArgumentsSignature (TypeArguments ta) {
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
