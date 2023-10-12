package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class FormalParameterBase extends SyntaxTreeNode {
    public FormalParameterBase (ParsePosition pos) {
	super (pos);
    }

    public abstract List<ParseTreeNode> getModifiers ();

    public abstract ParseTreeNode getType ();
}
