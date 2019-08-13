package org.khelekore.parjac2.parsetree;

import org.khelekore.parjac2.parser.Token;
import java.util.Collections;
import java.util.List;

public class TokenSyntaxTreeNode implements SyntaxTreeNode {
    private final Token token;
    private final Object value;

    public TokenSyntaxTreeNode (Token token, Object value) {
	this.token = token;
	this.value = value;
    }

    @Override public String getId () {
	return token.getName ();
    }

    @Override public Object getValue () {
	return value;
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
	return token + ":" + value;
    }
}