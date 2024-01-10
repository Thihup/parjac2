package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;

public interface NumericLiteral {
    double doubleValue ();
    float floatValue ();
    int intValue ();
    long longValue ();

    ParsePosition position ();
}
