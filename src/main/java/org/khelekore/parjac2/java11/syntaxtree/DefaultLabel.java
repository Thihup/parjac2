package org.khelekore.parjac2.java11.syntaxtree;

import org.khelekore.parjac2.parser.ParsePosition;

public class DefaultLabel extends SwitchLabel {
    public DefaultLabel (ParsePosition pos) {
	super (pos);
    }

    @Override public Object getValue () {
	return "default:";
    }
}
