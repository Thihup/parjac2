package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.parser.ParsePosition;

public interface Flagged {
    /** Get the combined flags of this thing, each bit is one flag */
    int getFlags ();

    /** Set the flags to a new value */
    void setFlags (int newFlags);

    default void makePublic () {
	setFlags (getFlags () | Flags.ACC_PUBLIC);
    }

    /** Get the position of this type */
    ParsePosition getPosition ();

    /** Get the expression type of this type */
    default ExpressionType getExpressionType () {
	return null;
    }
}
