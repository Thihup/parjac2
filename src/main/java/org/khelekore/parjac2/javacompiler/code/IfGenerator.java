package org.khelekore.parjac2.javacompiler.code;

import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.IntLiteral;
import org.khelekore.parjac2.javacompiler.JavaTokens;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.IfThenStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.LocalVariableDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ReturnStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.Ternary;
import org.khelekore.parjac2.javacompiler.syntaxtree.TwoPartExpression;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.Opcode;

public class IfGenerator {
    public static void handleTernary (MethodContentGenerator mcg, CodeBuilder cb, Ternary t) {
	handleGenericIfElse (mcg, cb, t.test (), t.thenPart (), t.elsePart ());
    }

    public static void handleIf (MethodContentGenerator mcg, CodeBuilder cb, IfThenStatement i) {
	handleGenericIfElse (mcg, cb, i.test (), i.thenPart (), i.elsePart ());
    }

    private static void handleGenericIfElse (MethodContentGenerator mcg, CodeBuilder cb, ParseTreeNode test,
					     ParseTreeNode thenPart, ParseTreeNode elsePart) {
	Opcode jumpInstruction = null;
	if (test instanceof TwoPartExpression tp) {
	    Token token = tp.token ();
	    if (isShortCircut (mcg, token)) {
		handleLogicalIf (mcg, cb, thenPart, elsePart, tp);
		return;
	    }

	    if (token == mcg.javaTokens ().INSTANCEOF) {
		mcg.handleStatements (cb, tp.part1 ());
		FullNameHandler check = FullNameHelper.type (tp.part2 ());
		cb.instanceof_ (ClassDescUtils.getClassDesc (check));
		jumpInstruction = Opcode.IFNE; // jump inverts, so we will use IFEQ
	    } else {
		handleTwoPartSetup (mcg, cb, tp);
		jumpInstruction = mcg.getForwardTwoPartJump (tp);
	    }
	} else {
	    mcg.handleStatements (cb, test);
	    jumpInstruction = Opcode.IFNE;
	}
	if (elsePart != null) {
	    cb.ifThenElse (jumpInstruction,
			   x -> mcg.handleStatements (x, thenPart),
			   x -> mcg.handleStatements (x, elsePart));
	} else {
	    cb.ifThen (jumpInstruction,
		       x -> mcg.handleStatements (x, thenPart));
	}
    }

    private static void handleLogicalIf (MethodContentGenerator mcg, CodeBuilder cb,
					 ParseTreeNode thenPart, ParseTreeNode elsePart, TwoPartExpression tp) {
	Label thenLabel = cb.newLabel ();
	Label elseLabel = cb.newLabel ();
	handleLogicalChain (mcg, cb, tp, thenLabel, elseLabel, false);
	handleIfThenParts (mcg, cb, thenPart, elsePart, thenLabel, elseLabel);
    }


    /*
      On their own:
      && first test: false -> jump to elseLabel
      && second test: false -> jump to elseLabel
      || first test: true -> jump to thenLabel
      || second test: false -> jump to elseLabel

      Howerver
      && inside || need change:
      && first test: false -> jump to elseLabel
      && second test: true -> jump to thenLabel
    */
    private static void handleLogicalChain (MethodContentGenerator mcg, CodeBuilder cb, TwoPartExpression tp,
					    Label thenLabel, Label elseLabel, boolean insideOr) {
	ParseTreeNode p1 = tp.part1 ();
	ParseTreeNode p2 = tp.part2 ();
	boolean isOr = tp.token () == mcg.javaTokens ().LOGICAL_OR;

	Label nextOption = null;
	if (isOr)
	    nextOption = cb.newLabel ();
	TwoPartExpression tp2 = null;
	if (p1 instanceof TwoPartExpression two)
	    tp2 = two;
	if (tp2 != null && isShortCircut (mcg, tp2.token ())) {
	    if (isOr)
		handleLogicalChain (mcg, cb, tp2, thenLabel, nextOption, true);
	    else
		handleLogicalChain (mcg, cb, tp2, thenLabel, elseLabel, false);
	} else if (tp2 != null && tp2.token () == mcg.javaTokens ().INSTANCEOF) {
	    handleInstanceOf (mcg, cb, tp2, elseLabel);
	} else if (tp2 != null) {
	    handleOtherTwoPart (mcg, cb, tp2);
	    cb.branchInstruction (mcg.getReverseTwoPartJump (tp2), elseLabel);
	} else {
	    mcg.handleStatements (cb, p1);
	    firstTest (cb, isOr, thenLabel, elseLabel);
	}
	if (isOr)
	    cb.labelBinding (nextOption);

	tp2 = p2 instanceof TwoPartExpression two ? two : null;
	if (tp2 != null && isShortCircut (mcg, tp2.token ())) {
	    handleLogicalChain (mcg, cb, tp2, thenLabel, elseLabel, false);
	} else if (tp2 != null && tp2.token () == mcg.javaTokens ().INSTANCEOF) {
	    handleInstanceOf (mcg, cb, tp2, elseLabel);
	} else if (tp2 != null) {
	    handleOtherTwoPart (mcg, cb, tp2);
	    cb.branchInstruction (mcg.getReverseTwoPartJump (tp2), elseLabel);
	} else {
	    mcg.handleStatements (cb, p2);
	    secondTest (cb, isOr, thenLabel, elseLabel, insideOr);
	}
    }

    public static Opcode handleOtherTwoPart (MethodContentGenerator mcg, CodeBuilder cb, TwoPartExpression two) {
	FullNameHandler fnt = two.fullName ();
	ParseTreeNode p1 = two.part1 ();
	mcg.handleStatements (cb, p1);
	CodeUtil.widen (cb, fnt, p1);

	ParseTreeNode p2 = two.part2 ();
	Opcode jumpInstruction = Opcode.IFEQ;
	if (!(fnt == FullNameHandler.BOOLEAN && p2 instanceof IntLiteral il && il.intValue () == 0)) {
	    mcg.handleStatements (cb, p2);
	    CodeUtil.widen (cb, fnt, p2);
	    jumpInstruction = mcg.getForwardTwoPartJump (two);
	}
	return jumpInstruction;
    }

    private static void firstTest (CodeBuilder cb, boolean isOr, Label thenLabel, Label elseLabel) {
	if (isOr)
	    cb.ifne (thenLabel);
	else
	    cb.ifeq (elseLabel);
    }

    private static void secondTest (CodeBuilder cb, boolean isOr, Label thenLabel, Label elseLabel, boolean insideOr) {
	if (isOr)
	    cb.ifeq (elseLabel);
	else if (insideOr)
	    cb.ifne (thenLabel);
	else
	    cb.ifeq (elseLabel);
    }

    private static void handleIfThenParts (MethodContentGenerator mcg, CodeBuilder cb,
					   ParseTreeNode thenPart, ParseTreeNode elsePart,
					   Label thenLabel, Label elseLabel) {
	cb.labelBinding (thenLabel);
	mcg.handleStatements (cb, thenPart);
	Label endLabel = cb.newLabel ();
	if (!endsWithReturn (thenPart))
	    cb.goto_ (endLabel);
	cb.labelBinding (elseLabel);
	if (elsePart != null)
	    mcg.handleStatements (cb, elsePart);
	cb.labelBinding (endLabel);
    }

    public static void handleInstanceOf (MethodContentGenerator mcg, CodeBuilder cb, TwoPartExpression tp, Label elseLabel) {
	mcg.handleStatements (cb, tp.part1 ());
	ParseTreeNode p2 = tp.part2 ();
	FullNameHandler check = FullNameHelper.type (p2);
	cb.instanceof_ (ClassDescUtils.getClassDesc (check));
	if (p2 instanceof LocalVariableDeclaration lvd) {
	    cb.ifeq (elseLabel);
	    LocalVariableHandler.handleLocalVariables (mcg, cb, lvd); // get slot for variable
	    mcg.handleStatements (cb, tp.part1 ());
	    cb.checkcast (ClassDescUtils.getClassDesc (check));
	    cb.astore (lvd.getDeclarators ().get (0).slot ()); // only one!
	}
    }

    public static void handleTwoPartSetup (MethodContentGenerator mcg, CodeBuilder cb, TwoPartExpression two) {
	FullNameHandler fnt = two.fullName ();
	ParseTreeNode p1 = two.part1 ();
	mcg.handleStatements (cb, p1);
	CodeUtil.widen (cb, fnt, p1);
	ParseTreeNode p2 = two.part2 ();
	mcg.handleStatements (cb, p2);
	CodeUtil.widen (cb, fnt, p2);
    }

    private static boolean endsWithReturn (ParseTreeNode p) {
	// TOOD: we might need more cases
	if (p instanceof ReturnStatement)
	    return true;
	if (p instanceof Block b) {
	    List<ParseTreeNode> ls = b.get ();
	    if (!ls.isEmpty () && ls.getLast () instanceof ReturnStatement)
		return true;
	}
	return false;
    }

    private static boolean isShortCircut (MethodContentGenerator mcg, Token token) {
	return token == mcg.javaTokens ().LOGICAL_AND || token == mcg.javaTokens ().LOGICAL_OR;
    }

    public static boolean isTrue (ParseTreeNode p, JavaTokens javaTokens) {
	return p instanceof TokenNode tn && tn.token () == javaTokens.TRUE;
    }
}
