package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.CatchClause;
import org.khelekore.parjac2.javacompiler.syntaxtree.Catches;
import org.khelekore.parjac2.javacompiler.syntaxtree.Finally;
import org.khelekore.parjac2.javacompiler.syntaxtree.TryStatement;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

public class TryGenerator {
    public static void handleTryStatement (MethodContentGenerator mcg, CodeBuilder cb, TryStatement t) {
	cb.trying (c -> handleBlockAndFinally (mcg, c, t.block (), t.finallyBlock ()),
		   c -> handleCatches (mcg, c, t.catches (), t.finallyBlock ()));

    }

    private static void handleCatches (MethodContentGenerator mcg, CodeBuilder.CatchBuilder cb, Catches catches, Finally finallyBlock) {
	if (catches != null) {
	    for (ParseTreeNode p : catches.get ()) {
		CatchClause cc = (CatchClause)p;
		ClassDesc exceptionType = null;
		cb.catching (exceptionType, c -> mcg.handleStatements (c, cc.block ()));
	    }
	}
	if (finallyBlock != null) {
	    cb.catchingAll (c -> handleFinally (mcg, c, finallyBlock));
	}
    }

    private static void handleBlockAndFinally (MethodContentGenerator mcg, CodeBuilder cb, Block block, Finally finallyBlock) {
	mcg.handleStatements (cb, block);
	if (finallyBlock != null)
	    mcg.handleStatements (cb, finallyBlock.block ());
    }

    private static void handleFinally (MethodContentGenerator mcg, CodeBuilder cb, Finally finallyBlock) {
	int exceptionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	cb.astore (exceptionSlot);
	mcg.handleStatements (cb, finallyBlock);
	cb.aload (exceptionSlot);
	cb.athrow ();
    }
}
