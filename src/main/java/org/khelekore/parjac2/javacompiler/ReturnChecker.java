package org.khelekore.parjac2.javacompiler;

import java.util.List;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.code.BytecodeBlockBase;
import org.khelekore.parjac2.javacompiler.code.IfGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.BasicForStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.ChangeByOneExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.DoStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnhancedForStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExpressionStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.IfThenStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.ReturnStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.StatementExpressionList;
import org.khelekore.parjac2.javacompiler.syntaxtree.ThrowStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.WhileStatement;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ReturnChecker extends SemanticCheckerBase {

    public ReturnChecker (ClassInformationProvider cip, JavaTokens javaTokens,
			  ParsedEntry tree, CompilerDiagnosticCollector diagnostics) {
	super (cip, javaTokens, tree, diagnostics);
    }

    @Override public void runCheck () {
	forAllTypes (this::check);
    }

    private void check (TypeDeclaration td) {
	td.getMethods ().forEach (this::checkMethod);
    }

    private void checkMethod (MethodDeclarationBase m) {
	// ClassSetter have already checked that the returned values are fine, so we only check
	// if we have return or throw as needed
	FullNameHandler res = m.result ();
	boolean returnRequired = res != FullNameHandler.VOID;

	ParseTreeNode body = m.getMethodBody ();
	if (body instanceof Block block) {
	    boolean endsWithReturnOrThrow = endsWithReturnOrThrow (block.get ());
	    if (!endsWithReturnOrThrow) {
		if (returnRequired)
		    error (block, "Method %s does not end with return or throw", m.name ());
		else
		    m.implicitVoidReturn (true);
	    }
	}
    }

    private boolean checkStatement (ParseTreeNode p) {
	if (p instanceof Block b) {
	    return endsWithReturnOrThrow (b.get ());
	}
	return endsWithReturnOrThrow (List.of (p));
    }

    private boolean endsWithReturnOrThrow (List<ParseTreeNode> ls) {
	boolean hasReturnOrThrow = false;
	for (int i = 0, s = ls.size (); i < s; i++) {
	    ParseTreeNode p = ls.get (i);
	    if (hasReturnOrThrow) {
		error (p, "Unreachable code");
		break;
	    }
	    switch (p) {
	    case ReturnStatement rs -> hasReturnOrThrow = true;
	    case ThrowStatement ts -> hasReturnOrThrow = true;
	    case BytecodeBlockBase bb -> hasReturnOrThrow = true;

	    case IfThenStatement ifts -> hasReturnOrThrow = handleIf (ifts);
	    case WhileStatement ws -> hasReturnOrThrow = handleWhile (ws);
	    case DoStatement ds -> hasReturnOrThrow = handleDo (ds);
	    case BasicForStatement f -> hasReturnOrThrow = handleBasicFor (f);
	    case EnhancedForStatement f -> hasReturnOrThrow = handleEnhancedFor (f);

	    case ExpressionStatement es -> handleIncrementDecrement (es);

	    default -> { /* empty */ }
	    }
	}
	return hasReturnOrThrow;
    }

    private boolean handleIf (IfThenStatement ifts) {
	boolean endsWithReturnOrThrow = checkStatement (ifts.thenPart ());
	ParseTreeNode ep = ifts.elsePart ();
	if (ep == null) {
	    if (IfGenerator.isTrue (ifts.test (), javaTokens))
		return endsWithReturnOrThrow;
	} else {
	    boolean endsWithReturnOrThrowElse = checkStatement (ep);
	    return endsWithReturnOrThrow && endsWithReturnOrThrowElse;
	}

	return false;
    }

    private boolean handleWhile (WhileStatement ws) {
	handleStatementList (ws.expression ());
	checkStatement (ws.statement ());
	return false;  // we do not know if it runs or not
    }

    private boolean handleDo (DoStatement ds) {
	handleStatementList (ds.expression ());
	checkStatement (ds.statement ());
	return false;
    }

    private boolean handleBasicFor (BasicForStatement bfs) {
	handleStatementList (bfs.forInit ());
	handleStatementList (bfs.forUpdate ());
	checkStatement (bfs.statement ());
	return false;  // we do not know if it runs or not
    }

    private void handleStatementList (ParseTreeNode p) {
	if (p instanceof StatementExpressionList sl)  {
	    List<ParseTreeNode> ls = sl.get ();
	    ls.forEach (this::handleIncrementDecrement);
	}
    }

    private boolean handleEnhancedFor (EnhancedForStatement efs) {
	checkStatement (efs.statement ());
	return false;
    }

    // a "i++;" on its own means that the code does not use the value
    private void handleIncrementDecrement (ExpressionStatement es) {
	handleIncrementDecrement (es.getStatement ());
    }

    private void handleIncrementDecrement (ParseTreeNode p) {
	if (p instanceof ChangeByOneExpression ce) {
	    ce.valueIsUsed (false);
	}
    }
}
