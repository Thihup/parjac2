package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.javacompiler.Flags;

public interface Flagged {
    /** Get the combined flags of this thing, each bit is one flag */
    int getFlags ();

    /** Set the flags to a new value */
    void setFlags (int newFlags);

    default void makePublic () {
	setFlags (getFlags () | Flags.ACC_PUBLIC);
    }
}
