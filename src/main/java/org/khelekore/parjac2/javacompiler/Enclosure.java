package org.khelekore.parjac2.javacompiler;

import java.util.Map;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;

public interface Enclosure<V extends VariableInfo> {
    boolean isStatic ();

    default TypeDeclaration td () { return null; }

    default FullNameHandler fqn () { return null; }

    default TypeParameter getTypeParameter (String id) { return null; }

    Map<String, V> getFields ();

    default V getField (String name) { return getFields ().get (name); }
}
