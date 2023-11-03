package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.CharLiteral;
import org.khelekore.parjac2.javacompiler.DoubleLiteral;
import org.khelekore.parjac2.javacompiler.FloatLiteral;
import org.khelekore.parjac2.javacompiler.IntLiteral;
import org.khelekore.parjac2.javacompiler.LongLiteral;
import org.khelekore.parjac2.javacompiler.StringLiteral;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

import static org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler.PrimitiveType;
import static org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler.*;

public class FullNameHelper {

    private static final Map<String, PrimitiveType> SIGNATURE_LOOKUP = new HashMap<> ();
    private static final Map<PrimitiveType, List<PrimitiveType>> ALLOWED_UPCASTS = new HashMap<> ();

    static {
	SIGNATURE_LOOKUP.put (BYTE.signature (), BYTE);
	SIGNATURE_LOOKUP.put (SHORT.signature (), SHORT);
	SIGNATURE_LOOKUP.put (CHAR.signature (), CHAR);
	SIGNATURE_LOOKUP.put (INT.signature (), INT);
	SIGNATURE_LOOKUP.put (LONG.signature (), LONG);
	SIGNATURE_LOOKUP.put (FLOAT.signature (), FLOAT);
	SIGNATURE_LOOKUP.put (DOUBLE.signature (), DOUBLE);
	SIGNATURE_LOOKUP.put (BOOLEAN.signature (), BOOLEAN);
	SIGNATURE_LOOKUP.put (VOID.signature (), VOID);

	ALLOWED_UPCASTS.put (BYTE, Arrays.asList (SHORT, INT, LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (SHORT, Arrays.asList (INT, LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (CHAR, Arrays.asList (INT, LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (INT, Arrays.asList (LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (FLOAT, Arrays.asList (DOUBLE));
    };

    public static PrimitiveType getPrimitiveType (String signature) {
	return SIGNATURE_LOOKUP.get (signature);
    }

    public static PrimitiveType getPrimitive (TokenNode t) {
	return switch (t.token ().getName ()) {
	case "byte" -> BYTE;
	case "short" -> SHORT;
	case "char" -> CHAR;
	case "int" -> INT;
	case "long" -> LONG;
	case "float" -> FLOAT;
	case "double" -> DOUBLE;
	case "boolean" -> BOOLEAN;
	case "void" -> VOID;
	default -> throw new IllegalArgumentException ("Unhandled type: " + t + ", " + t.getClass ().getName ());
	};
    }

    public static FullNameHandler type (ParseTreeNode p) {
	return switch (p) {
	case ClassType ct -> ct.getFullNameHandler ();
	case MethodInvocation mi -> mi.result ();
	case DottedName an -> an.getFullNameHandler ();
	case StringLiteral s -> JL_STRING;
	case CharLiteral x ->  CHAR;
	case IntLiteral x ->  INT;
	case LongLiteral x ->  LONG;
	case FloatLiteral x ->  FLOAT;
	case DoubleLiteral x ->  DOUBLE;
	case TokenNode tn -> getPrimitive (tn);
	case ArrayType at -> arrayOf (type (at.getType ()), at.rank ());
	case ArrayAccess aa -> aa.type ();
	case CastExpression ce -> type (ce.baseType ());
	case ClassLiteral cl -> JL_CLASS;
	case ThisPrimary tp -> tp.type ();
	case FieldAccess fa -> fa.getFullName ();
	case Ternary t -> t.type ();
	case TwoPartExpression tp -> tp.type ();
	default -> throw new IllegalArgumentException ("Unhandled type: " + p + ", " + p.getClass ().getName ());
	};
    }
}
