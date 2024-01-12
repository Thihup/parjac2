package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.CatchClause;
import org.khelekore.parjac2.javacompiler.syntaxtree.Catches;
import org.khelekore.parjac2.javacompiler.syntaxtree.Finally;
import org.khelekore.parjac2.javacompiler.syntaxtree.TryStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.UnannClassType;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.TypeKind;

public class TryGenerator {
    public static void handleTryStatement (MethodContentGenerator mcg, CodeBuilder cb, TryStatement t) {
	// We would love to use cb.trying, but it does not deal with finally-blocks well.
	// We get Exception tables that include all code in the inlined finally-block.
	Label afterTry = cb.newLabel ();
	Label tryStart = cb.newBoundLabel ();
	mcg.handleStatements (cb, t.block ());
	Label tryEnd = cb.newBoundLabel ();
	Finally finallyStatements = t.finallyBlock ();
	if (finallyStatements != null)
	    mcg.handleStatements (cb, finallyStatements);
	cb.goto_ (afterTry);

	int exceptionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	Catches cc = t.catches ();
	if (cc != null) {
	    for (ParseTreeNode p : cc.get ()) {
		Label handler = cb.newBoundLabel ();
		cb.astore (exceptionSlot);

		CatchClause c = (CatchClause)p;
		// TODO: deal with more types.
		UnannClassType uct = c.firstType ();
		ClassDesc exceptionType = ClassDescUtils.getClassDesc (uct);
		mcg.handleStatements (cb, c.block ());
		cb.exceptionCatch (tryStart, tryEnd, handler, exceptionType);
		// TODO: probably inline finally-block here
		cb.goto_ (afterTry);
	    }
	}

	if (finallyStatements != null) {
	    Label handler = cb.newBoundLabel ();
	    cb.astore (exceptionSlot);
	    mcg.handleStatements (cb, finallyStatements);
	    cb.aload (exceptionSlot);
	    cb.athrow ();
	    cb.exceptionCatchAll (tryStart, tryEnd, handler);
	}

	cb.labelBinding (afterTry);
    }
}
