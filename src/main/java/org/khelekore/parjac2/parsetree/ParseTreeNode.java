package org.khelekore.parjac2.parsetree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;

public interface ParseTreeNode {
    /** Get the id of this tree node, typically the name of the token or rule */
    Object getId ();
    /** Get the value the lexer produced for this token, may be null */
    Object getValue ();
    /** Is this node a rule? */
    boolean isRule ();
    /** Is this node a token? */
    boolean isToken ();
    /** Get the child nodes for this token, empty list for leaf nodes */
    List<ParseTreeNode> getChildren ();
    /** Get the parse position for this node of the tree. */
    ParsePosition getPosition ();
    /** Visit the child nodes */
    void visitChildNodes (NodeVisitor v);
}
