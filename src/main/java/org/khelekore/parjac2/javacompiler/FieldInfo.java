package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.FieldDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclarator;

// TODO: figure out if we still need FieldInformation

public record FieldInfo (String name, FieldDeclarationBase fd, VariableDeclarator vd) {
    // empty
}
