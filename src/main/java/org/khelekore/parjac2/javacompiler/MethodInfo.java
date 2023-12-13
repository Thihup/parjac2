package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.parser.ParsePosition;

import io.github.dmlloyd.classfile.MethodSignature;
import io.github.dmlloyd.classfile.Signature;

/** description of a method in a class. Used to make compiled
 *  methods and resource methods be handled the same way.
 */
public interface MethodInfo {

    // class that the method is declared in
    FullNameHandler owner ();

    default ClassDesc ownerDesc () {
	return ClassDescUtils.getClassDesc (owner ());
    }

    MethodTypeDesc methodTypeDesc ();

    /** Return the position of this method, may be null if from a class resource */
    ParsePosition position ();

    int flags ();

    default boolean isStatic () {
	return Flags.isStatic (flags ());
    }

    String name ();

    int numberOfArguments ();

    // return type
    FullNameHandler result ();

    FullNameHandler parameter (int i);

    String signature ();

    default FullNameHandler genericResult (Map<String, FullNameHandler> genericTypes) {
	String signature = signature ();
	if (signature != null) {
	    // TODO: possibly something like this: ??
	    MethodSignature ms = MethodSignature.parseFrom (signature);
	    Signature result = ms.result ();
	    return genericTypes.get (((Signature.TypeVarSig)result).identifier ());
	}
	return result ();
    }
}
