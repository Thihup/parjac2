package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public interface NumericLiteral {
    double doubleValue ();
    float floatValue ();
    int intValue ();
    long longValue ();

    ParsePosition position ();

    ParseTreeNode negate ();
}
