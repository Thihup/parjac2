package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public record FieldInfo (Type fieldType, String name, ParsePosition pos, int flags, ParseTreeNode type, int arrayRank) implements VariableInfo {
    @Override public FullNameHandler typeName () {
	return FullNameHelper.type (type);
    }
}
