package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;

/** description of a method in a class. Used to make compiled
 *  methods and resource methods be handled the same way.
 */
public interface MethodInfo {
    int flags ();

    String name ();

    int numberOfArguments ();

    FullNameHandler result ();
    // TODO: return type
    // TODO: argument list
    // TODO: generic types?
}
