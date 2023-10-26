package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.FieldDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclarator;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public record FieldInfo (String name, FieldDeclarationBase fd, VariableDeclarator vd) implements VariableInfo {

    public int flags () {
	return fd.flags ();
    }

    public ParseTreeNode getType () {
	return fd.getType ();
    }
}
