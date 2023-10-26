package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ExpressionType {

    public static final PrimitiveExpressionType BYTE = new PrimitiveExpressionType ("B");
    public static final PrimitiveExpressionType SHORT = new PrimitiveExpressionType ("S");
    public static final PrimitiveExpressionType CHAR = new PrimitiveExpressionType ("C");
    public static final PrimitiveExpressionType INT = new PrimitiveExpressionType ("I");
    public static final PrimitiveExpressionType LONG = new PrimitiveExpressionType ("J");
    public static final PrimitiveExpressionType FLOAT = new PrimitiveExpressionType ("F");
    public static final PrimitiveExpressionType DOUBLE = new PrimitiveExpressionType ("D");
    public static final PrimitiveExpressionType BOOLEAN = new PrimitiveExpressionType ("Z");

    public static final PrimitiveExpressionType VOID = new PrimitiveExpressionType ("V");
    public static final ExpressionType NULL = new NullExpressionType ();

    private static Map<FullNameHandler, ObjectExpressionType> objectExpressionTypeCache = new ConcurrentHashMap<> ();
    public static final ExpressionType STRING = getObjectType (FullNameHandler.JL_STRING);
    public static final ExpressionType OBJECT = getObjectType (FullNameHandler.JL_OBJECT);
    public static final ExpressionType CLASS = getObjectType (FullNameHandler.JL_CLASS);

    private static final PrimitiveExpressionType[] primitives =
    { BYTE, SHORT, CHAR, INT, LONG, FLOAT, DOUBLE, BOOLEAN, VOID };

    private final static Map<PrimitiveExpressionType, List<PrimitiveExpressionType>> ALLOWED_UPCASTS = new HashMap<> ();
    private final static Set<ExpressionType> INTEGRAL_TYPES = new HashSet<> ();
    private final static Set<ExpressionType> NUMERIC_TYPES = new HashSet<> ();
    private final static Map<ExpressionType, String> PRIMITIVE_NAMES = new HashMap<> ();
    static {
	ALLOWED_UPCASTS.put (BYTE, Arrays.asList (SHORT, INT, LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (SHORT, Arrays.asList (INT, LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (CHAR, Arrays.asList (INT, LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (INT, Arrays.asList (LONG, FLOAT, DOUBLE));
	ALLOWED_UPCASTS.put (FLOAT, Arrays.asList (DOUBLE));

	INTEGRAL_TYPES.add (BYTE);
	INTEGRAL_TYPES.add (SHORT);
	INTEGRAL_TYPES.add (CHAR);
	INTEGRAL_TYPES.add (INT);
	INTEGRAL_TYPES.add (LONG);

	NUMERIC_TYPES.addAll (INTEGRAL_TYPES);
	NUMERIC_TYPES.add (FLOAT);
	NUMERIC_TYPES.add (DOUBLE);

	PRIMITIVE_NAMES.put (BYTE, "byte");
	PRIMITIVE_NAMES.put (SHORT, "short");
	PRIMITIVE_NAMES.put (CHAR, "char");
	PRIMITIVE_NAMES.put (INT, "int");
	PRIMITIVE_NAMES.put (LONG, "long");
	PRIMITIVE_NAMES.put (FLOAT, "float");
	PRIMITIVE_NAMES.put (DOUBLE, "double");
	PRIMITIVE_NAMES.put (BOOLEAN, "boolean");
	PRIMITIVE_NAMES.put (VOID, "void");
    }

    public static boolean mayBeAutoCasted (ExpressionType from, ExpressionType to) {
	List<PrimitiveExpressionType> l = ALLOWED_UPCASTS.get (from);
	return l != null && l.contains (to);
    }

    private static class PrimitiveExpressionType extends ExpressionType {
	private final String primitiveType;

	public PrimitiveExpressionType (String primitiveType) {
	    this.primitiveType = primitiveType;
	}

	@Override public boolean equals (Object o) {
	    return o == this;
	}

	@Override public String toString () {
	    return PRIMITIVE_NAMES.get (this);
	}

	@Override public boolean isPrimitiveType () {
	    return true;
	}

	@Override public boolean isClassType () {
	    return false;
	}

	@Override public String getDollarName () {
	    return primitiveType;
	}

	@Override public String getSlashName () {
	    return primitiveType;
	}

	@Override public String getDescriptor () {
	    return primitiveType;
	}

	/*
	@Override public boolean match (Type t) {
	    return type == t.getSort () || mayBeAutoCasted (this, t);
	}
	*/
    }

    private static class ObjectExpressionType extends ExpressionType {
	private final FullNameHandler fnh;

	public ObjectExpressionType (FullNameHandler fnh) {
	    if (fnh == null)
		throw new NullPointerException ("Given name may not be null");
	    this.fnh = fnh;
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    ObjectExpressionType e = (ObjectExpressionType)o;
	    return fnh.equals (e.fnh);
	}

	@Override public int hashCode () {
	    return fnh.hashCode ();
	}

	@Override public String toString () {
	    return fnh.getFullDotName ();
	}

	@Override public FullNameHandler getFullNameHandler () {
	    return fnh;
	}

	@Override public String getDollarName () {
	    return fnh.getFullDollarName ();
	}

	@Override public String getSlashName () {
	    return fnh.getSlashName ();
	}

	@Override public String getDescriptor () {
	    return "L" + getSlashName () + ";";
	}

	/* qwerty
	@Override public boolean match (Type t) {
	    return t.getSort () == Type.OBJECT && t.getInternalName ().equals (slashName);
	}
	*/
    }

    private static class NullExpressionType extends ExpressionType {
	@Override public String toString () {
	    return "null";
	}

	@Override public boolean isClassType () {
	    throw new IllegalStateException ("Not applicable");
	}

	@Override public String getDollarName () {
	    throw new IllegalStateException ("Not applicable");
	}

	@Override public String getSlashName () {
	    throw new IllegalStateException ("Not applicable");
	}

	@Override public String getDescriptor () {
	    throw new IllegalStateException ("Not applicable");
	}

	/* qwerty
	@Override public boolean match (Type t) {
	    throw new IllegalStateException ("Not applicable");
	}
	*/
    }

    private static class ArrayExpressionType extends ExpressionType {
	private final ExpressionType base;
	private final int dims;

	public ArrayExpressionType (ExpressionType base, int dims) {
	    if (base == null)
		throw new NullPointerException ("Array base type may not be null");
	    this.base = base;
	    this.dims = dims;
	}

	@Override public ExpressionType arrayAccess () {
	    int newDims = dims - 1;
	    if (newDims == 0)
		return base;
	    return new ArrayExpressionType (base, newDims);
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    ArrayExpressionType e = (ArrayExpressionType)o;
	    return dims == e.dims && base.equals (e.base);
	}

	@Override public int hashCode () {
	    return base.hashCode () * 31 + dims;
	}

	@Override public String toString () {
	    return base.toString () + repeat ("[]", dims);
	}

	@Override public boolean isArray () {
	    return true;
	}

	@Override public int getArrayDimensions () {
	    return dims;
	}

	@Override public ExpressionType getArrayBaseType () {
	    return base;
	}

	@Override public FullNameHandler getFullNameHandler () {
	    return FullNameHandler.ofDollarName (base.getDollarName () + repeat ("[]", dims));
	}

	@Override public String getDollarName () {
	    return base.getDollarName ();
	}

	@Override public String getSlashName () {
	    return base.getSlashName ();
	}

	@Override public String getDescriptor () {
	    return repeat ("[", dims) + base.getDescriptor ();
	}

	/* qwerty
	@Override public boolean match (Type t) {
	    return t.getSort () == Type.ARRAY &&
		t.getInternalName ().equals (getSlashName ()) &&
		t.getDimensions () == dims;
	}
	*/
    }

    public static ExpressionType getObjectType (FullNameHandler fnh) {
	return objectExpressionTypeCache.computeIfAbsent (fnh, ObjectExpressionType::new);
    }

    public static ExpressionType array (ExpressionType base, int dims) {
	return new ArrayExpressionType (base, dims);
    }

    public ExpressionType arrayAccess () {
	throw new IllegalStateException ("Not an array type: "  + this);
    }

    /* qwerty
    public static ExpressionType get (Type t) {
	if (t.getSort () == Type.ARRAY) {
	    ExpressionType base = get (t.getElementType ());
	    return array (base, t.getDimensions ());
	}
	if (t.getSort () == Type.OBJECT)
	    return new ObjectExpressionType (t.getInternalName ().replace ('/', '.'));
	for (PrimitiveExpressionType et : primitives)
	    if (et.type == t.getSort ())
		return et;
	throw new IllegalArgumentException ("Unknown type: " + t + ", t.sort: " + t.getSort ());
    }
    */

    public static ExpressionType bigger (ExpressionType e1, ExpressionType e2) {
	if (mayBeAutoCasted (e1, e2))
	    return e2;
	if (e1 == NULL)
	    return e2;
	return e1;
    }

    /** Check if this ExpressionType is a primitive type */
    public boolean isPrimitiveType () {
	return false;
    }

    public boolean isClassType () {
	return true;
    }

    public boolean isArray () {
	return false;
    }

    public int getArrayDimensions () {
	return 0;
    }

    public ExpressionType getArrayBaseType () {
	throw new IllegalStateException ("Not an array type");
    }

    /** Get the name of the class this type represents. */
    public FullNameHandler getFullNameHandler () {
	throw new IllegalStateException ("Not a class type: " + this + ", " + this.getClass ().getName ());
    }

    public abstract String getDollarName ();

    /** Get the '/'-separated name of a type */
    public abstract String getSlashName ();

    public abstract String getDescriptor ();

    /** Check if this type match the given Type */
    /* qwerty
    public abstract boolean match (Type t);
    */

    /* qwerty
    public static boolean mayBeAutoCasted (ExpressionType from, Type t) {
	List<PrimitiveExpressionType> l = ALLOWED_UPCASTS.get (from);
	if (l == null)
	    return false;
	for (PrimitiveExpressionType et : l)
	    if (et.type == t.getSort ())
		return true;
	return false;
    }
    */

    private static String repeat (String s, int times) {
	StringBuilder sb = new StringBuilder ();
	for (int i = 0; i < times; i++)
	    sb.append (s);
	return sb.toString ();
    }

    public boolean isIntegralType () {
	return INTEGRAL_TYPES.contains (this);
    }

    public boolean isNumericType () {
	return NUMERIC_TYPES.contains (this);
    }

    public boolean isBooleanType () {
	return this == BOOLEAN;
    }
}
