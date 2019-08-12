package org.khelekore.parjac2.parser;

import java.util.BitSet;

public interface Lexer {
    /** Check if there are any more tokens.
     * @return true if there are more tokens to be read
     */
    boolean hasMoreTokens ();

    /** Scan for the next token.
     * @param wantedTokens a BitSet where each bit represents a token identifier
     * @return the next scanned token (does not have to be one of the wanted tokens).
     */
    Token nextToken (BitSet wantedTokens);

    /** Get the current token value, if any */
    Object getCurrentValue ();

    /** Get the current position.
     */
    ParsePosition getParsePosition ();
}
