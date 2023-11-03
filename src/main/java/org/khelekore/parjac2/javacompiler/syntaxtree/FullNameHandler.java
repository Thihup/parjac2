package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.concurrent.ConcurrentHashMap;

import org.khelekore.parjac2.javacompiler.ClassInformationProvider;
import org.khelekore.parjac2.javacompiler.GenericTypeHelper;

public interface FullNameHandler {

    public static final FullNameHandler JL_OBJECT = FullNameHandler.ofSimpleClassName ("java.lang.Object");
    public static final FullNameHandler JL_CLASS = FullNameHandler.ofSimpleClassName ("java.lang.Class");
    public static final FullNameHandler JL_ENUM = FullNameHandler.ofSimpleClassName ("java.lang.Enum");
    public static final FullNameHandler JL_RECORD = FullNameHandler.ofSimpleClassName ("java.lang.Record");
    public static final FullNameHandler JL_STRING = FullNameHandler.ofSimpleClassName ("java.lang.String");

    public static final PrimitiveType BYTE = new PrimitiveType ("B", "byte");
    public static final PrimitiveType SHORT = new PrimitiveType ("S", "short");
    public static final PrimitiveType CHAR = new PrimitiveType ("C", "char");
    public static final PrimitiveType INT = new PrimitiveType ("I", "int");
    public static final PrimitiveType LONG = new PrimitiveType ("J", "long");
    public static final PrimitiveType FLOAT = new PrimitiveType ("F", "float");
    public static final PrimitiveType DOUBLE = new PrimitiveType ("D", "double");
    public static final PrimitiveType BOOLEAN = new PrimitiveType ("Z", "boolean");

    public static final PrimitiveType VOID = new PrimitiveType ("V", "void");

    public static final FullNameHandler NULL = new NullType ();

    public enum Type {
	OBJECT, PRIMITIVE;
    }

    /** Check if this is a primitive or an object type */
    default Type getType () {
	return Type.OBJECT;
    }

    /** Get the dotted name, for ecample foo.bar.Baz.Quox */
    String getFullDotName ();

    /** Get the dotted name, for ecample foo.bar.Baz$Quox */
    String getFullDollarName ();

    /** Get the dotted name, for ecample foo/bar/Baz$Quox */
    default String getSlashName () {
	return getFullDollarName ().replace ('.', '/');
    }

    /** Check if the types described by this namae has any generic types. */
    default boolean hasGenericType () {
	return false;
    }

    /** Get the array rank */
    default int rank () {
	return 0;
    }

    /** Check if this type is an array type */
    default boolean isArray () {
	return rank () > 0;
    }

    default boolean isPrimitive () {
	return false;
    }

    /** Get the signature of this type.
     * Note the signature will be something like "java/lang/String" or "C", it will not
     * start with "L" or end with ";" since those depends on context of the signature.
     */
    default String getSignature (GenericTypeHelper gth, ClassInformationProvider cip,
				 boolean shortForm, TypeArguments ta) {
	String slashName = getSlashName ();
	return ta != null ? slashName + gth.getTypeArgumentsSignature (ta, cip, shortForm) : slashName;
    }

    /** Get a full name handler from the dotted name of an outer class "foo.bar.Baz" */
    static FullNameHandler ofSimpleClassName (String dotName) {
	return DotNameHandler.of (dotName);
    }

    /** Get a full name handler from the dollar name of an outer class "foo.bar.Baz$Quox" */
    static FullNameHandler ofDollarName (String dollarName) {
	return SimpleNameHandler.of (dollarName.replace ('$', '.'), dollarName);
    }

    static FullNameHandler of (String dot, String dollar) {
	return SimpleNameHandler.of (dot, dollar);
    }

    /** Get the FullNameHandler for an inner class to this outer class */
    default FullNameHandler getInnerClass (String id) {
	String dot = getFullDotName () + "." + id;
	String dollar = getFullDollarName () + "$" + id;
	return SimpleNameHandler.of (dot, dollar);
    }

    static FullNameHandler arrayOf (FullNameHandler fn, int rank) {
	return new ArrayHandler (fn, rank);
    }

    default FullNameHandler array (int rank) {
	return new ArrayHandler (this, rank);
    }

    public record DotNameHandler (String dotName)  implements FullNameHandler {
	private final static ConcurrentHashMap<String, DotNameHandler> nameCache = new ConcurrentHashMap<> ();

	public static DotNameHandler of (String dotName) {
	    return nameCache.computeIfAbsent (dotName, DotNameHandler::new);
	}

	@Override public String getFullDotName () {
	    return dotName;
	}

	@Override public String getFullDollarName () {
	    return dotName;
	}
    }

    public record SimpleNameHandler (String dotName, String dollarName)  implements FullNameHandler {
	private final static ConcurrentHashMap<String, SimpleNameHandler> nameCache = new ConcurrentHashMap<> ();

	public static SimpleNameHandler of (String dotName, String dollarName) {
	    return nameCache.computeIfAbsent (dollarName, n -> new SimpleNameHandler (dotName, dollarName));
	}

	@Override public String getFullDotName () {
	    return dotName;
	}

	@Override public String getFullDollarName () {
	    return dollarName;
	}
    }

    public record PrimitiveType (String signature, String name) implements FullNameHandler {

	@Override public Type getType () {
	    return Type.PRIMITIVE;
	}

	@Override public String getFullDotName () {
	    return name;
	}

	@Override public String getFullDollarName () {
	    return name;
	}

	@Override public String getSlashName () {
	    return name;
	}

	@Override public String getSignature (GenericTypeHelper gth, ClassInformationProvider cip,
					      boolean shortForm, TypeArguments ta) {
	    return signature;
	}

	public String getSignature () {
	    return signature;
	}

	@Override public boolean isPrimitive () {
	    return true;
	}
    }

    public class NullType implements FullNameHandler {
	@Override public String getFullDotName () {
	    return "null";
	}

	@Override public String getFullDollarName () {
	    return "null";
	}

	@Override public String getSlashName () {
	    throw new IllegalStateException ("Not applicable");
	}

	@Override public String getSignature (GenericTypeHelper gth, ClassInformationProvider cip,
					      boolean shortForm, TypeArguments ta) {
	    return "V";
	}
    }

    public record ArrayHandler (FullNameHandler fn, int rank) implements FullNameHandler {
	@Override public String getFullDotName () {
	    return "[" + fn.getFullDotName ();
	}

	@Override public String getFullDollarName () {
	    return "[" + fn.getFullDollarName ();
	}

	@Override public String getSlashName () {
	    return "[" + fn.getSlashName ();
	}

	@Override public String getSignature (GenericTypeHelper gth, ClassInformationProvider cip,
					      boolean shortForm, TypeArguments ta) {
	    return "[" + fn.getSignature (gth, cip, shortForm, ta);
	}
    }
}
