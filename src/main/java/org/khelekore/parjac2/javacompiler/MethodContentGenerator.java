package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;

public interface MethodContentGenerator {

    void handleStatements (CodeBuilder cb, ParseTreeNode statement);
}
