package org.khelekore.parjac2.parsetree;

import org.khelekore.parjac2.parser.Token;
import java.util.Collections;
import java.util.List;

public class TokenSyntaxTreeNode implements SyntaxTreeNode {
    private final Token token;

    public TokenSyntaxTreeNode (Token token) {
	this.token = token;
    }

    @Override public String getId () {
	return token.getName ();
    }

    @Override public Object getValue () {
	return null;
    }

    @Override public boolean isRule () {
	return false;
    }

    @Override public boolean isToken () {
	return true;
    }

    @Override public List<SyntaxTreeNode> getChildren () {
	return Collections.emptyList ();
    }

    @Override public String toString () {
	return token.getName ();
    }
}