package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;

public class TypeIdentifier extends Identifier {
    public TypeIdentifier (Token token, String value, ParsePosition pos) {
	super (token, value, pos);
    }
}
