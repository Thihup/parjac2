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
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.TypeKind;

public class TryGenerator {
    public static void handleTryStatement (MethodContentGenerator mcg, CodeBuilder cb, TryStatement t) {
	// We would love to use cb.trying, but it does not deal with finally-blocks well.
	// We get Exception tables that include all code in the finally-block.
	/*
	cb.trying (c -> handleBlockAndFinally (mcg, c, t.block (), t.finallyBlock ()),
		   c -> handleCatches (mcg, c, t.catches (), t.finallyBlock ()));
	*/
	Label afterTry = cb.newLabel ();
	Label tryStart = cb.newBoundLabel ();
	mcg.handleStatements (cb, t.block ());
	Label tryEnd = cb.newBoundLabel ();
	Finally f = t.finallyBlock ();
	if (f != null)
	    mcg.handleStatements (cb, f);
	cb.goto_ (afterTry);

	Catches cc = t.catches ();
	if (cc != null) {
	    for (ParseTreeNode p : cc.get ()) {
		Label handler = cb.newBoundLabel ();
		CatchClause c = (CatchClause)p;
		ClassDesc exceptionType = null;
		mcg.handleStatements (cb, c.block ());
		cb.exceptionCatch (tryStart, tryEnd, handler, exceptionType);
		// TODO: probably inline finally-block here
		cb.goto_ (afterTry);
	    }
	}

	if (f != null) {
	    Label handler = cb.newBoundLabel ();
	    int exceptionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	    cb.astore (exceptionSlot);
	    mcg.handleStatements (cb, f);
	    cb.aload (exceptionSlot);
	    cb.athrow ();
	    cb.exceptionCatchAll (tryStart, tryEnd, handler);
	}

	cb.labelBinding (afterTry);
    }

}
