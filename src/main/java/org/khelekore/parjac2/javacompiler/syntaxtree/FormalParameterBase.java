package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.VariableInfo;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class FormalParameterBase extends SyntaxTreeNode implements VariableInfo {
    private int slot;

    public FormalParameterBase (ParsePosition pos) {
	super (pos);
    }

    public abstract List<ParseTreeNode> getModifiers ();

    @Override public int flags () {
	return Flags.ACC_PUBLIC; // not sure, but treat as public for easier handling
    }

    @Override public abstract String name ();

    @Override public abstract ParseTreeNode type ();

    public void slot (int slot) {
	this.slot = slot;
    }

    public int slot () {
	return slot;
    }
}
