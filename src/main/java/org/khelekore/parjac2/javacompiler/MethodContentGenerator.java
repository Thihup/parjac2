package org.khelekore.parjac2.javacompiler;

import java.util.List;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;

public interface MethodContentGenerator {

    default void handleStatements (CodeBuilder cb, ParseTreeNode statement) {
	handleStatements (cb, List.of (statement));
    }

    void handleStatements (CodeBuilder cb, List<? extends ParseTreeNode> statements);
}
