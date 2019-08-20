package org.khelekore.parjac2.parsetree;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;

/** Wildcard interpreted as a given token.
 */
public class WildcardNode extends TokenNode {
    public WildcardNode (Token token, ParsePosition pos) {
	super (token, pos);
    }

    @Override public String getId () {
	return "Wildcard: " + super.getId ();
    }
}