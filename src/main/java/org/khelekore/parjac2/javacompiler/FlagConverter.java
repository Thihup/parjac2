package org.khelekore.parjac2.javacompiler;

import java.util.HashMap;
import java.util.Map;

import org.khelekore.parjac2.parser.Token;

import static org.khelekore.parjac2.javacompiler.Flags.*;

public class FlagConverter {
    private Map<Token, Integer> tokenToValue = new HashMap<> ();
    private Map<Integer, Token> valueToToken = new HashMap<> ();

    public FlagConverter (JavaTokens tokens) {
	store (tokens.ABSTRACT, ACC_ABSTRACT);
	store (tokens.FINAL, ACC_FINAL);
	store (tokens.NATIVE, ACC_NATIVE);
	store (tokens.PRIVATE, ACC_PRIVATE);
	store (tokens.PROTECTED, ACC_PROTECTED);
	store (tokens.PUBLIC, ACC_PUBLIC);
	store (tokens.STATIC, ACC_STATIC);
	store (tokens.STRICTFP, ACC_STRICT);
	store (tokens.DEFAULT, ACC_DEFAULT);
	store (tokens.SYNCHRONIZED, ACC_SYNCHRONIZED);
	store (tokens.VOLATILE, ACC_VOLATILE);
    }

    private void store (Token t, int value) {
	tokenToValue.put (t, value);
	valueToToken.put (value, t);
    }

    public int getValue (Token t) {
	Integer i = tokenToValue.get (t);
	return i == null ? 0 : i;
    }

    public Token getToken (int value) {
	return valueToToken.get (value);
    }

    public String getTokenNameString (int mask) {
	StringBuilder sb = new StringBuilder ();
	while (mask > 0) {
	    int first = Integer.lowestOneBit (mask);
	    mask &= ~first;
	    if (sb.length () > 0)
		sb.append (", ");
	    sb.append (getToken (first).getName ());
	}
	return sb.toString ();
    }
}
