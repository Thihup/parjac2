package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;

import io.github.dmlloyd.classfile.ClassSignature;

record LookupResult (boolean found, int accessFlags, FullNameHandler fullName, ClassSignature signature) {
    public static final LookupResult NOT_FOUND = new LookupResult (false, 0, null, null);
}
