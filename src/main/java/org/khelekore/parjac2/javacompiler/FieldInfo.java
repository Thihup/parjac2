package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public record FieldInfo (String name, ParsePosition pos, int flags, ParseTreeNode type, int arrayRank) implements VariableInfo {
}
