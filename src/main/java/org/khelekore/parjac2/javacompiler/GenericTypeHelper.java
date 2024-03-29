package org.khelekore.parjac2.javacompiler;

import java.util.List;

import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Dims;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.PrimitiveType;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeArguments;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeBound;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameters;
import org.khelekore.parjac2.javacompiler.syntaxtree.Wildcard;
import org.khelekore.parjac2.javacompiler.syntaxtree.WildcardBounds;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class GenericTypeHelper {

    public String getGenericType (ParseTreeNode tn, ClassInformationProvider cip, boolean shortForm) {
	if (tn instanceof ClassType ct) {
	    TypeParameter tp = ct.getTypeParameter ();
	    if (tp != null)
		return "T" + tp.getId () + ";";
	    return "L" + ct.fullName ().getSignature (this, cip, shortForm, ct.getTypeArguments ()) + ";";
	} else if (tn instanceof PrimitiveType pt) {
	    return pt.fullName ().getSignature ();
	} else if (tn instanceof TokenNode tkn) {
	    return FullNameHelper.getPrimitive (tkn.token ()).getSignature ();
	} else if (tn instanceof ArrayType at) {
	    ParseTreeNode type = at.getType ();
	    Dims dims = at.getDims ();
	    return "[".repeat (dims.rank ()) + getGenericType (type, cip, shortForm);
	}
	// TODO: not sure how we get here.
	//return tn.getExpressionType ().getDescriptor ();
	System.err.println ("GenericTypeHelper: Got unhandled type: " + tn.getClass ().getName ());
	return tn.getValue ().toString ();
    }

    public String getTypeArgumentsSignature (TypeArguments ta, ClassInformationProvider cip, boolean shortForm) {
	StringBuilder sb = new StringBuilder ();
	sb.append ("<");
	for (ParseTreeNode tn : ta.getTypeArguments ()) {
	    switch (tn) {
	    //TODO: we also need to handle type and additional bounds
	    case Wildcard w -> sb.append (getGenericType (w, cip, shortForm));
	    default -> sb.append (getGenericType (tn, cip, shortForm));
	    }
	}
	sb.append (">");
	return sb.toString ();
    }

    private String getGenericType (Wildcard w, ClassInformationProvider cip, boolean shortForm) {
	WildcardBounds b = w.getBounds ();
	if (b == null)
	    return "*";
	return "+" + getGenericType (w.getBounds ().getClassType (), cip, shortForm);
    }

    public void appendTypeParametersSignature (StringBuilder sb, TypeParameters tps, ClassInformationProvider cip, boolean shortForm) {
	if (tps == null)
	    return;
	sb.append ("<");
	for (TypeParameter tp : tps.get ()) {
	    if (shortForm) {
		sb.append ("T").append (tp.getId ()).append (";");
	    } else {
		sb.append (tp.getId ());
		TypeBound b = tp.getTypeBound ();
		if (b != null) {
		    ClassType bt = b.getType ();
		    appendType (sb, bt, cip, shortForm);
		    List<ClassType> ls = b.getAdditionalBounds ();
		    if (ls != null) {
			ls.forEach (ct -> appendType (sb, ct, cip, shortForm));
		    }
		} else {
		    sb.append (":Ljava/lang/Object;");
		}
	    }
	}
	sb.append (">");
    }

    private void appendType (StringBuilder sb, ClassType ct, ClassInformationProvider cip, boolean shortForm) {
	sb.append (cip.isInterface (ct.getFullDotName ()) ? "::" : ":");
	sb.append (getGenericType (ct, cip, shortForm));
    }
}
