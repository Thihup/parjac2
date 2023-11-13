package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclarator;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public record LocalVariable (String name, VariableDeclarator vd, ParseTreeNode type) implements VariableInfo {

    @Override public VariableInfo.Type fieldType () {
	return VariableInfo.Type.LOCAL;
    }

    @Override public int flags () {
	return Flags.ACC_PUBLIC;
    }

    public int rank () {
	return vd.rank ();
    }

    public int slot () {
	return vd.slot ();
    }
}
