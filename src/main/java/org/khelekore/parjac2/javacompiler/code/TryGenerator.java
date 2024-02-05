package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.CatchClause;
import org.khelekore.parjac2.javacompiler.syntaxtree.Catches;
import org.khelekore.parjac2.javacompiler.syntaxtree.Finally;
import org.khelekore.parjac2.javacompiler.syntaxtree.ResourceList;
import org.khelekore.parjac2.javacompiler.syntaxtree.TryStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.UnannClassType;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.TypeKind;

public class TryGenerator {

    private static final MethodTypeDesc AUTO_CLOSE_SIGNATURE =
	MethodTypeDesc.of (ConstantDescs.CD_void);

    private static final ClassDesc AUTO_CLOSEABLE = ClassDesc.of ("java.lang.AutoCloseable");

    private static final MethodTypeDesc SUPPRESS_SIGNATURE =
	MethodTypeDesc.of (ConstantDescs.CD_void, ConstantDescs.CD_Throwable);

    public static void handleTryStatement (MethodContentGenerator mcg, CodeBuilder cb, TryStatement t) {
	Label start = cb.newBoundLabel ();

	Label afterTry = cb.newLabel ();
	InfoHolder next = new InfoHolder ();
	cb.block (bb -> {
		List<Integer> resourceSlots = setupResources (mcg, bb, t);
		// We would love to use cb.trying, but it does not deal with finally-blocks well.
		// We get Exception tables that include all code in the inlined finally-block.
		Label tryStart = bb.newBoundLabel ();
		mcg.handleStatements (bb, t.block ());
		Label tryEnd = bb.newBoundLabel ();
		next.label = closeResourcesAndHandleExceptions (bb, resourceSlots, tryStart, tryEnd);
		next.exceptionSlot = bb.allocateLocal (TypeKind.ReferenceType);
	    });

	Label finallyStart = cb.newBoundLabel ();
	Finally finallyBlock = t.finallyBlock ();
	if (finallyBlock != null) {
	    cb.labelBinding (next.label);
	    mcg.handleStatements (cb, finallyBlock);
	}
	cb.goto_ (afterTry);

	int exceptionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	Label finallyAfterCatch = finallyBlock != null ? cb.newLabel () : null;
	ExceptionInfo ei = new ExceptionInfo (exceptionSlot, start, finallyStart, finallyAfterCatch);
	handleCatchClauses (mcg, cb, t, afterTry, ei, finallyBlock);

	if (finallyBlock != null) {
	    cb.labelBinding (finallyAfterCatch);
	    cb.astore (next.exceptionSlot);
	    mcg.handleStatements (cb, finallyBlock);
	    cb.aload (next.exceptionSlot);
	    cb.athrow ();
	    cb.exceptionCatchAll (start, finallyStart, finallyAfterCatch);
	}

	if (finallyBlock == null)
	    cb.labelBinding (next.label);
	cb.labelBinding (afterTry);
    }

    private static List<Integer> setupResources (MethodContentGenerator mcg, CodeBuilder cb, TryStatement t) {
	ResourceList rs = t.resources ();
	List<Integer> resourceSlots = new ArrayList<Integer> ();
	if (rs != null) {
	    for (ParseTreeNode p : rs.get ()) {
		mcg.handleStatements (cb, p);
		int slot = cb.allocateLocal (TypeKind.ReferenceType);
		resourceSlots.add (slot);
		cb.astore (slot);
	    }
	}
	return resourceSlots;
    }

    private static Label closeResourcesAndHandleExceptions (CodeBuilder cb, List<Integer> resourceSlots, Label tryStart, Label tryEnd) {
	Label next = closeResources (cb, resourceSlots);
	if (!resourceSlots.isEmpty ()) {
	    Label exceptionHandler = cb.newBoundLabel ();
	    cb.exceptionCatch (tryStart, tryEnd, exceptionHandler, ConstantDescs.CD_Throwable);

	    int exceptionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	    cb.astore (exceptionSlot);
	    Label closeStart = cb.newBoundLabel ();
	    Label toThrow = closeResources (cb, resourceSlots);
	    Label closeEnd = cb.newBoundLabel ();

	    // TODO: closeStart and closeEnd are not correct, they should be tighter
	    cb.exceptionCatch (closeStart, closeEnd, closeEnd, ConstantDescs.CD_Throwable);
	    int suppressed = cb.allocateLocal (TypeKind.ReferenceType);
	    cb.astore (suppressed);
	    cb.aload (exceptionSlot);
	    cb.aload (suppressed);
	    cb.invokevirtual (ConstantDescs.CD_Throwable, "addSuppressed", SUPPRESS_SIGNATURE);

	    cb.labelBinding (toThrow);
	    cb.aload (exceptionSlot);
	    cb.athrow ();
	}
	return next;
    }

    private static Label closeResources (CodeBuilder cb, List<Integer> resourceSlots) {
	Label next = cb.newLabel ();
	for (int i = resourceSlots.size () - 1; i >= 0; i--) {
	    int slot = resourceSlots.get (i);
	    cb.aload (slot);
	    cb.if_null (next);
	    cb.aload (slot);
	    cb.invokeinterface (AUTO_CLOSEABLE, "close", AUTO_CLOSE_SIGNATURE);
	    cb.goto_ (next);
	    if (i > 0) {
		cb.labelBinding (next);
		next = cb.newLabel ();
	    }
	}
	return next;
    }

    private static void handleCatchClauses (MethodContentGenerator mcg, CodeBuilder cb,
					    TryStatement t, Label afterTry, ExceptionInfo ei,
					    Finally finallyBlock) {
	Catches cc = t.catches ();
	if (cc != null) {
	    for (ParseTreeNode p : cc.get ()) {
		Label handler = cb.newBoundLabel ();
		cb.astore (ei.exceptionSlot ());

		CatchClause c = (CatchClause)p;
		// TODO: deal with more types.
		UnannClassType uct = c.firstType ();
		ClassDesc exceptionType = ClassDescUtils.getClassDesc (uct);
		mcg.handleStatements (cb, c.block ());
		cb.exceptionCatch (ei.start (), ei.end (), handler, exceptionType);
		Label catchEnd = cb.newBoundLabel ();

		if (finallyBlock != null)
		    mcg.handleStatements (cb, finallyBlock);

		if (ei.finallyAfterCatch != null) {
		    cb.exceptionCatchAll (handler, catchEnd, ei.finallyAfterCatch);
		}
		cb.goto_ (afterTry);
	    }
	}
    }

    private static class InfoHolder {
	Label label;
	int exceptionSlot;
    }

    private record ExceptionInfo (int exceptionSlot, Label start, Label end, Label finallyAfterCatch) {
	// empty
    }
}
