package org.khelekore.parjac2.javacompiler.syntaxtree;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public interface SwitchPart {
    /** Get the code that handles this switch part */
    ParseTreeNode handler ();

}
