package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public abstract class FlaggedBase extends SyntaxTreeNode implements Flagged {
    protected int flags;

    public FlaggedBase (ParsePosition pos) {
	super (pos);
    }

    /** Get the flags for this type */
    @Override public int getFlags () {
	return flags;
    }

    /** Set the flags for this type */
    @Override public void setFlags (int flags) {
	this.flags = flags;
    }
}
