package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.IntLiteral;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.BasicForStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.DoStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnhancedForStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.LocalVariableDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TwoPartExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.UnaryExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclarator;
import org.khelekore.parjac2.javacompiler.syntaxtree.WhileStatement;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.Opcode;
import io.github.dmlloyd.classfile.TypeKind;

public class LoopGenerator {

    public static void handleBasicFor (MethodContentGenerator mcg, CodeBuilder cb, BasicForStatement bfs) {
	ParseTreeNode forInit = bfs.forInit ();
	ParseTreeNode expression = bfs.expression ();
	ParseTreeNode forUpdate = bfs.forUpdate ();
	ParseTreeNode statement = bfs.statement ();

	if (forInit != null)
	    mcg.handleStatements (cb, forInit);
	Label lExp = cb.newBoundLabel ();
	Label lEnd = cb.newLabel ();
	mcg.registerJumpTargets ("", lExp, lEnd);
	if (expression != null) {
	    handleExitTest (mcg, cb, expression, lEnd);
	}
	mcg.handleStatements (cb, statement);
	mcg.handleStatements (cb, forUpdate);
	cb.goto_ (lExp); // what about goto_w?
	cb.labelBinding (lEnd);
    }

    public static void handleEnhancedFor (MethodContentGenerator mcg, CodeBuilder cb, EnhancedForStatement efs) {
	ParseTreeNode exp = efs.expression ();
	FullNameHandler fn = FullNameHelper.type (exp);
	if (fn.isArray ()) {
	    handleArrayLoop (mcg, cb, efs, fn);
	} else {
	    handleIteratorLoop (mcg, cb, efs, fn);
	}
    }

    /*
      static int a (int[] x) {          Transformed into this:
      int r = 0;                    int r = 0;
                                    int[] xc = x; int s = xc.length;
      for (int a : x)               for (int i = 0; i < s; i++) {
          r += a;                       a = xc[i]; r += a;
      }                             }
      return r;                     return r;
      }}
    */
    private static void handleArrayLoop (MethodContentGenerator mcg, CodeBuilder cb, EnhancedForStatement efs, FullNameHandler fn) {
	LocalVariableDeclaration lv = efs.localVariable ();
	List<VariableDeclarator> vds = lv.getDeclarators ();
	if (vds.size () > 1)
	    throw new IllegalStateException ("Unable to handle more than one variable in EnhancedForStatement.");
	VariableDeclarator vd = vds.get (0);
	FullNameHandler varName = FullNameHelper.type (lv);
	TypeKind varKind = FullNameHelper.getTypeKind (varName);

	TypeKind kind = FullNameHelper.getTypeKind (fn);
	ArrayInfo ai = ArrayInfo.create (cb, kind);
	arrayLoopSetup (mcg, cb, efs, ai);                    // copy array, store length

	Label loopLabel = cb.newBoundLabel ();
	Label endLabel = cb.newLabel ();
	mcg.registerJumpTargets ("", loopLabel, endLabel);
	arrayLoopIndexCheck (cb, ai, endLabel);          // i < s
	storeArrayLoopValue (mcg, cb, ai, lv, vd, varKind);   // a = xc[i]
	mcg.handleStatements (cb, efs.statement ());
	cb.iinc (ai.indexSlot (), 1);                    // i++
	cb.goto_ (loopLabel); // what about goto_w?
	cb.labelBinding (endLabel);
    }

    private static void arrayLoopSetup (MethodContentGenerator mcg, CodeBuilder cb, EnhancedForStatement efs, ArrayInfo ai) {
	mcg.handleStatements (cb, efs.expression ());
	cb.astore (ai.arrayCopySlot ());
	cb.aload (ai.arrayCopySlot ());
	cb.arraylength ();
	cb.istore (ai.lengthSlot ());
	cb.iconst_0 ();
	cb.istore (ai.indexSlot ());
    }

    private static void arrayLoopIndexCheck (CodeBuilder cb, ArrayInfo ai, Label endLabel) {
	cb.iload (ai.indexSlot ());
	cb.iload (ai.lengthSlot ());
	cb.if_icmpeq (endLabel);
    }

    private static void storeArrayLoopValue (MethodContentGenerator mcg, CodeBuilder cb, ArrayInfo ai,
					     LocalVariableDeclaration lv, VariableDeclarator vd, TypeKind varKind) {
	cb.aload (ai.arrayCopySlot ());
	cb.iload (ai.indexSlot ());
	cb.iaload ();
	LocalVariableHandler.handleLocalVariables (mcg, cb, lv);
	int varSlot = vd.slot ();
	cb.storeInstruction (varKind, varSlot);
    }

    private record ArrayInfo (int arrayCopySlot, int lengthSlot, int indexSlot) {
	public static ArrayInfo create (CodeBuilder cb, TypeKind kind) {
	    return new ArrayInfo (cb.allocateLocal (kind), cb.allocateLocal (TypeKind.IntType), cb.allocateLocal (TypeKind.IntType));
	}
    }

    private static void handleIteratorLoop (MethodContentGenerator mcg, CodeBuilder cb, EnhancedForStatement efs, FullNameHandler fn) {
	int iteratorSlot = cb.allocateLocal (TypeKind.ReferenceType);
	mcg.handleStatements (cb, efs.expression ());
	ClassDesc owner = ClassDescUtils.getClassDesc (fn);
	ClassDesc iteratorDesc = ClassDesc.of ("java.util.Iterator");
	MethodTypeDesc type = MethodTypeDesc.of (iteratorDesc);
	cb.invokeinterface (owner, "iterator", type);
	cb.astore (iteratorSlot);

	Label loopLabel = cb.newBoundLabel ();
	Label endLabel = cb.newLabel ();
	mcg.registerJumpTargets ("", loopLabel, endLabel);

	cb.aload (iteratorSlot);
	type = MethodTypeDesc.ofDescriptor ("()Z");
	cb.invokeinterface (iteratorDesc, "hasNext", type);
	cb.ifeq (endLabel);

	cb.aload (iteratorSlot);
	type = MethodTypeDesc.of (ConstantDescs.CD_Object);
	cb.invokeinterface (iteratorDesc, "next", type);
	// TODO: add: cb.checkcast (genericType);

	LocalVariableDeclaration lv = efs.localVariable ();
	LocalVariableHandler.handleLocalVariables (mcg, cb, lv);
	int varSlot = lv.getDeclarators ().get (0).slot ();
	cb.storeInstruction (TypeKind.ReferenceType, varSlot);

	mcg.handleStatements (cb, efs.statement ());

	cb.goto_ (loopLabel); // what about goto_w?
	cb.labelBinding (endLabel);
    }

    public static void handleWhile (MethodContentGenerator mcg, CodeBuilder cb, WhileStatement ws) {
	Label loopLabel = cb.newBoundLabel ();
	Label endLabel = cb.newLabel ();
	mcg.registerJumpTargets ("", loopLabel, endLabel);

	handleExitTest (mcg, cb, ws.expression (), endLabel);
	mcg.handleStatements (cb, ws.statement ());
	cb.goto_ (loopLabel);
	cb.labelBinding (endLabel);
    }

    public static void handleDo (MethodContentGenerator mcg, CodeBuilder cb, DoStatement ws) {
	Label loopLabel = cb.newBoundLabel ();
	Label endLabel = cb.newLabel ();
	mcg.registerJumpTargets ("", loopLabel, endLabel);
	mcg.handleStatements (cb, ws.statement ());
	handleRunAgainTest (mcg, cb, ws.expression (), loopLabel);
	cb.labelBinding (endLabel);
    }

    private static void handleExitTest (MethodContentGenerator mcg, CodeBuilder cb, ParseTreeNode exp, Label endLabel) {
	handleTest (mcg, cb, exp, endLabel, REVERSE_LOOP_TESTER);
    }

    private static void handleRunAgainTest (MethodContentGenerator mcg, CodeBuilder cb, ParseTreeNode exp, Label loopStart) {
	handleTest (mcg, cb, exp, loopStart, RUN_AGAIN_LOOP_TESTER);
    }

    private static void handleTest (MethodContentGenerator mcg, CodeBuilder cb, ParseTreeNode exp, Label label, LoopTester tester) {
	Opcode operator = Opcode.IFEQ;
	if (exp instanceof UnaryExpression) {
	    mcg.handleStatements (cb, exp);
	    operator = tester.unaryOpcode ();
	} else if (exp instanceof TwoPartExpression tp) {
	    mcg.handleStatements (cb, tp.part1 ());

	    ParseTreeNode p2 = tp.part2 ();
	    if (p2 instanceof IntLiteral il && il.intValue () == 0) {
		operator = tester.zeroJump (mcg, tp.token ());
	    } else {
		mcg.handleStatements (cb, tp.part2 ());
		operator = tester.twoPartJump (mcg, tp);
	    }
	}
	cb.branchInstruction (operator, label);
    }

    private interface LoopTester {
	Opcode unaryOpcode ();
	Opcode zeroJump (MethodContentGenerator mcg, Token t);
	Opcode twoPartJump (MethodContentGenerator mcg, TwoPartExpression tp);
    }

    private static class ReverseLoopTester implements LoopTester {
	@Override public Opcode unaryOpcode () { return Opcode.IFNE; }
	@Override public Opcode zeroJump (MethodContentGenerator mcg, Token t) { return mcg.getReverseZeroJump (t); }
	@Override public Opcode twoPartJump (MethodContentGenerator mcg, TwoPartExpression tp) { return mcg.getReverseTwoPartJump (tp); }
    }

    private static final LoopTester REVERSE_LOOP_TESTER = new ReverseLoopTester ();

    private static class RunAgainLoopTester implements LoopTester {
	@Override public Opcode unaryOpcode () { return Opcode.IFEQ; }
	@Override public Opcode zeroJump (MethodContentGenerator mcg, Token t) { return mcg.getForwardZeroJump (t); }
	@Override public Opcode twoPartJump (MethodContentGenerator mcg, TwoPartExpression tp) { return mcg.getForwardTwoPartJump (tp); }
    }

    private static final LoopTester RUN_AGAIN_LOOP_TESTER = new RunAgainLoopTester ();
}
