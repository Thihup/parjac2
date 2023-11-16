package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;

/** description of a method in a class. Used to make compiled
 *  methods and resource methods be handled the same way.
 */
public interface MethodInfo {

    // class that the method is declared in
    FullNameHandler owner ();

    default ClassDesc ownerDesc () {
	return ClassDesc.of (owner ().getFullDollarName ());
    }

    MethodTypeDesc methodTypeDesc ();

    int flags ();

    String name ();

    int numberOfArguments ();

    // return type
    FullNameHandler result ();

    FullNameHandler parameter (int i);

    // TODO: generic types?
}
