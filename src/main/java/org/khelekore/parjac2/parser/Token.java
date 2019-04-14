package org.khelekore.parjac2.parser;

public class Token {
    private final String tokenName;
    private final int tokenId;

    public Token (String tokenName, int tokenId) {
	this.tokenName = tokenName;
	this.tokenId = tokenId;
    }

    public String getName () {
	return tokenName;
    }

    public int getId () {
	return tokenId;
    }

    public int hashCode () {
	return tokenId;
    }

    public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	return ((Token)o).tokenId == tokenId;
    }

    @Override public String toString () {
	return "Token{" + tokenName + ", " + tokenId + "}";
    }
}