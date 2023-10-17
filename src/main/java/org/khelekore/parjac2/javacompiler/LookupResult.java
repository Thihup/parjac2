package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;

record LookupResult (boolean found, int accessFlags, FullNameHandler fullName) {
    public static final LookupResult NOT_FOUND = new LookupResult (false, 0, null);
}
