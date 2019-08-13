package org.khelekore.parjac2.parsetree;

import java.util.List;

public interface SyntaxTreeNode {
    Object getId ();
    Object getValue ();
    boolean isRule ();
    boolean isToken ();
    List<SyntaxTreeNode> getChildren ();
}
