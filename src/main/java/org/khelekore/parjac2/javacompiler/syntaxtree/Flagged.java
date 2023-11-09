package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.parser.ParsePosition;

public interface Flagged {
    /** Get the combined flags of this thing, each bit is one flag */
    int flags ();

    /** Set the flags to a new value */
    void setFlags (int newFlags);

    default void makePublic () {
	setFlags (flags () | Flags.ACC_PUBLIC);
    }

    /** Get the position of this type */
    ParsePosition position ();
}
