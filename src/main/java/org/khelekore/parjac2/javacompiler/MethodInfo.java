package org.khelekore.parjac2.javacompiler;

/** description of a method in a class. Used to make compiled
 *  methods and resource methods be handled the same way.
 */
public interface MethodInfo {
    int flags ();

    String name ();

    // TODO: return type
    // TODO: argument list
    // TODO: generic types?
}
