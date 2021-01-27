package org.khelekore.parjac2.parser;

import java.util.BitSet;

import org.khelekore.parjac2.parsetree.TokenNode;

public interface Lexer {
    /** Check if there are any more tokens.
     * @return true if there are more tokens to be read
     */
    boolean hasMoreTokens ();

    /** Scan for the next token.
     * @param wantedTokens a BitSet where each bit represents a token identifier
     * @return the next scanned tokens (does not have to be one of the wanted tokens).
     */
    BitSet nextToken (BitSet wantedTokens);

    /** Get the current token value
     *  If the current token is a set of different then this may return any one of them
     */
    TokenNode getCurrentValue ();

    /** Convert the given node to the actually wanted token */
    TokenNode toCorrectType (TokenNode n, Token wantedActualToken);

    /** Get the current position.
     */
    ParsePosition getParsePosition ();

    /** Get the last produced error, if any */
    String getError ();
}
