package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.javacompiler.ClassInformationProvider;
import org.khelekore.parjac2.javacompiler.GenericTypeHelper;

public interface FullNameHandler {

    public static final FullNameHandler JL_OBJECT = FullNameHandler.ofSimpleClassName ("java.lang.Object");
    public static final FullNameHandler JL_CLASS = FullNameHandler.ofSimpleClassName ("java.lang.Class");
    public static final FullNameHandler JL_ENUM = FullNameHandler.ofSimpleClassName ("java.lang.Enum");
    public static final FullNameHandler JL_RECORD = FullNameHandler.ofSimpleClassName ("java.lang.Record");
    public static final FullNameHandler JL_STRING = FullNameHandler.ofSimpleClassName ("java.lang.String");

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

    default String getSignature (GenericTypeHelper gth, ClassInformationProvider cip,
				 boolean shortForm, TypeArguments ta) {
	String slashName = getSlashName ();
	return ta != null ? slashName + gth.getTypeArgumentsSignature (ta, cip, shortForm) : slashName;
    }

    /** Get a full name handler from the dotted name of an outer class "foo.bar.Baz" */
    static FullNameHandler ofSimpleClassName (String dotName) {
	return new DotNameHandler (dotName);
    }

    /** Get a full name handler from the dollar name of an outer class "foo.bar.Baz$Quox" */
    static FullNameHandler ofDollarNAme (String dollarName) {
	return new SimpleNameHandler (dollarName.replace ('$', '.'), dollarName);
    }

    static FullNameHandler of (String dot, String dollar) {
	return new SimpleNameHandler (dot, dollar);
    }

    /** Get the FullNameHandler for an inner class to this outer class */
    default FullNameHandler getInnerClass (String id) {
	String dot = getFullDotName () + "." + id;
	String dollar = getFullDollarName () + "$" + id;
	return new SimpleNameHandler (dot, dollar);
    }

    public record DotNameHandler (String dotName)  implements FullNameHandler {
	public String getFullDotName () {
	    return dotName;
	}

	public String getFullDollarName () {
	    return dotName;
	}
    }

    public record SimpleNameHandler (String dotName, String dollarName)  implements FullNameHandler {
	public String getFullDotName () {
	    return dotName;
	}

	public String getFullDollarName () {
	    return dollarName;
	}
    }
}

