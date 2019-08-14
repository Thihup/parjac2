package org.khelekore.parjac2.parsetree;

import org.khelekore.parjac2.parser.Token;

/** Wildcard interpreted as a given token.
 */
public class WildcardSyntaxTreeNode extends TokenSyntaxTreeNode {
    public WildcardSyntaxTreeNode (Token token) {
	super (token);
    }

    @Override public String getId () {
	return "Wildcard: " + super.getId ();
    }
}