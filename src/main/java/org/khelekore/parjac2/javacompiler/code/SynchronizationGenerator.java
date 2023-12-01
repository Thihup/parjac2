package org.khelekore.parjac2.javacompiler.code;

import java.util.Optional;

import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.SynchronizedStatement;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.TypeKind;

public class SynchronizationGenerator {
    public static void handleSynchronized (MethodContentGenerator mcg, CodeBuilder cb, SynchronizedStatement ss) {
	mcg.handleStatements (cb, ss.expression ());
	cb.dup ();
	int expressionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	cb.astore (expressionSlot);
	cb.monitorenter ();

	Label endLabel = cb.newLabel ();
	Label monitorStart = cb.newBoundLabel ();
	mcg.handleStatements (cb, ss.block ());

	cb.aload (expressionSlot);
	cb.monitorexit ();
	Label monitorEnd = cb.newBoundLabel ();
	cb.goto_ (endLabel);

	// TODO: can we alwyas specify all exceptions here?
	Label handlerStart = cb.newBoundLabel ();
	cb.exceptionCatch (monitorStart, monitorEnd, handlerStart, Optional.empty ());
	int exceptionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	cb.astore (exceptionSlot);
	cb.aload (expressionSlot);
	cb.monitorexit ();
	Label handlerEnd = cb.newBoundLabel ();
	cb.aload (exceptionSlot);
	cb.athrow ();
	cb.exceptionCatch (handlerStart, handlerEnd, handlerStart, Optional.empty ());

	cb.labelBinding (endLabel);
    }
}
