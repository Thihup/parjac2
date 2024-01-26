package org.khelekore.parjac2.javacompiler;

import java.util.List;

import org.khelekore.parjac2.javacompiler.syntaxtree.TwoPartExpression;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.Opcode;

public interface MethodContentGenerator {

    default void handleStatements (CodeBuilder cb, ParseTreeNode statement) {
	handleStatements (cb, List.of (statement));
    }

    void handleStatements (CodeBuilder cb, List<? extends ParseTreeNode> statements);

    Opcode getReverseZeroJump (Token t);
    Opcode getReverseTwoPartJump (TwoPartExpression t);

    Opcode getForwardZeroJump (Token t);
    Opcode getForwardTwoPartJump (TwoPartExpression t);

    void registerJumpTargets (String id, ParseTreeNode loop, Label next, Label end);
    void jumpToNext (CodeBuilder cb, String id);
    void jumpToEnd (CodeBuilder cb, String id);

    JavaTokens javaTokens ();
}
