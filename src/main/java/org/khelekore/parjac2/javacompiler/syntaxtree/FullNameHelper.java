package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.lang.constant.ClassDesc;
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
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

import io.github.dmlloyd.classfile.TypeKind;

import static org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler.Primitive;
import static org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler.*;

public class FullNameHelper {

    private static final Map<String, Primitive> SIGNATURE_LOOKUP = new HashMap<> ();
    private static final Map<Primitive, List<Primitive>> ALLOWED_UPCASTS = new HashMap<> ();
    private static final Map<Primitive, FullNameHandler> AUTO_BOX = new HashMap<> ();
    private static final Map<FullNameHandler, TypeKind> toTypeKind = new HashMap<> ();

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

	AUTO_BOX.put (BYTE, FullNameHandler.ofSimpleClassName ("java.lang.Byte"));
	AUTO_BOX.put (SHORT, FullNameHandler.ofSimpleClassName ("java.lang.Short"));
	AUTO_BOX.put (CHAR, FullNameHandler.ofSimpleClassName ("java.lang.Char"));
	AUTO_BOX.put (INT, FullNameHandler.ofSimpleClassName ("java.lang.Integer"));
	AUTO_BOX.put (LONG, FullNameHandler.ofSimpleClassName ("java.lang.Long"));
	AUTO_BOX.put (FLOAT, FullNameHandler.ofSimpleClassName ("java.lang.Float"));
	AUTO_BOX.put (DOUBLE, FullNameHandler.ofSimpleClassName ("java.lang.Double"));

	toTypeKind.put (BYTE, TypeKind.ByteType);
	toTypeKind.put (SHORT, TypeKind.ShortType);
	toTypeKind.put (CHAR, TypeKind.CharType);
	toTypeKind.put (INT, TypeKind.IntType);
	toTypeKind.put (LONG, TypeKind.LongType);
	toTypeKind.put (FLOAT, TypeKind.FloatType);
	toTypeKind.put (DOUBLE, TypeKind.DoubleType);
	toTypeKind.put (BOOLEAN, TypeKind.BooleanType);
	toTypeKind.put (VOID, TypeKind.VoidType);
    };

    public static Primitive getPrimitiveType (String signature) {
	return SIGNATURE_LOOKUP.get (signature);
    }

    public static Primitive getPrimitive (Token t) {
	return switch (t.getName ()) {
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
	case ClassType ct -> ct.fullName ();
	case PrimitiveType pt -> pt.fullName ();
	case ArrayCreationExpression ac -> ac.fullName ();
	case MethodInvocation mi -> mi.result ();
	case DottedName an -> an.fullName ();
	case StringLiteral s -> JL_STRING;
	case CharLiteral x ->  CHAR;
	case IntLiteral x ->  INT;
	case LongLiteral x ->  LONG;
	case FloatLiteral x ->  FLOAT;
	case DoubleLiteral x ->  DOUBLE;
	case TokenNode tn when tn.token ().getName ().equals ("null") -> NULL;
	case TokenNode tn when tn.token ().getName ().equals ("true") -> BOOLEAN;
	case TokenNode tn when tn.token ().getName ().equals ("false") -> BOOLEAN;
	case TokenNode tn -> getPrimitive (tn.token ());
	case ArrayType at -> arrayOf (type (at.getType ()), at.rank ());
	case ArrayAccess aa -> aa.type ();
	case Assignment a -> type (a.lhs ());
	case LocalVariableDeclaration lv -> type (lv.getType ());
	case CastExpression ce -> type (ce.baseType ());
	case ClassInstanceCreationExpression cice -> cice.type ();
	case ClassLiteral cl -> JL_CLASS;
	case ThisPrimary tp -> tp.type ();
	case FieldAccess fa -> fa.fullName ();
	case Ternary t -> t.type ();
	case TwoPartExpression tp -> tp.fullName ();
	case UnaryExpression ue when ue.operator ().getName ().equals ("-") -> type (ue.expression ());
	case FormalParameterBase fp -> fp.typeName ();
	case UnaryExpression ue -> type (ue.expression ());
	case LambdaExpression le -> le.type ();
	case MethodReference mr -> mr.type ();
	case PostIncrementExpression pie -> type (pie.expression ());
	case PreIncrementExpression pie -> type (pie.expression ());
	default -> throw new IllegalArgumentException ("Unhandled type: " + p + ", " + p.getClass ().getName ());
	};
    }

    public static FullNameHandler wider (FullNameHandler f1, FullNameHandler f2) {
	if (f1 == FullNameHandler.DOUBLE || f2 == FullNameHandler.DOUBLE)
	    return FullNameHandler.DOUBLE;
	if (f1 == FullNameHandler.FLOAT || f2 == FullNameHandler.FLOAT)
	    return FullNameHandler.FLOAT;
	if (f1 == FullNameHandler.LONG || f2 == FullNameHandler.LONG)
	    return FullNameHandler.LONG;
	if (f1 == FullNameHandler.INT || f2 == FullNameHandler.INT)
	    return FullNameHandler.INT;
	return f1; // not sure what we do here
    }

    public static boolean mayAutoCastPrimitives (FullNameHandler from, FullNameHandler to) {
	List<Primitive> ls = ALLOWED_UPCASTS.get (from);
	return ls != null && ls.contains (to);
    }

    public static boolean canAutoBoxTo (FullNameHandler from, FullNameHandler to) {
	if (!from.isPrimitive ())
	    return false;
	FullNameHandler fn = AUTO_BOX.get (from);
	return fn != null && fn.equals (to);
    }

    public static boolean canAutoUnBoxTo (FullNameHandler from, FullNameHandler to) {
	if (!to.isPrimitive ())
	    return false;
	FullNameHandler fn = AUTO_BOX.get (to);
	return fn != null && fn.equals (from);
    }

    public static FullNameHandler getAutoBoxOption (FullNameHandler fn) {
	return AUTO_BOX.get (fn);
    }

    public static TypeKind getTypeKind (FullNameHandler fn) {
	if (fn == FullNameHandler.NULL || fn.getType () == FullNameHandler.Type.OBJECT)
	    return TypeKind.ReferenceType;
	TypeKind tk = toTypeKind.get (fn);
	if (tk == null)
	    throw new NullPointerException ("Unable to find TypeKind for: " + fn.getFullDotName ());
	return tk;
    }

    public static FullNameHandler get (ClassDesc cd) {
	return ofDescriptor (cd.descriptorString ());
    }

    public static FullNameHandler ofDescriptor (String desc) {
	if (desc.length () == 1)
	    return SIGNATURE_LOOKUP.get (desc);
	if (desc.charAt (0) == 'L' && desc.charAt (desc.length () - 1) == ';') {
	    String slashName = desc.substring (1, desc.length () - 1);
	    String dollarName = slashName.replace ('/', '.');
	    String dotName = dollarName.replace ('$', '.');
	    return FullNameHandler.of (dotName, dollarName);
	}
	int i = 0;
	for (; i < desc.length () && desc.charAt (i) == '['; i++) {
	    // empty
	}
	if (i > 0) {
	    FullNameHandler fn = ofDescriptor (desc.substring (i));
	    return fn.array (i);
	}
	throw new IllegalArgumentException ("Unable to parse: " + desc);
    }
}
