package org.khelekore.parjac2.javacompiler;

import java.util.List;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.code.BytecodeBlockBase;
import org.khelekore.parjac2.javacompiler.code.IfGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.*;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ReturnChecker extends SemanticCheckerBase {

    private enum State {
	RETURNS,      // returns or throws
	SOFT_RETURN,  // returns or throws, but we treat it softly, see handleIf
	NO_RETURN;    // does not return or throw

	public State and (State other) {
	    return values () [Math.max (ordinal (), other.ordinal ())];
	}

	public State or (State other) {
	    return values () [Math.min (ordinal (), other.ordinal ())];
	}
    }

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
	    State endsWithReturnOrThrow = endsWithReturnOrThrow (block.get ());
	    if (endsWithReturnOrThrow != State.RETURNS) {
		if (returnRequired)
		    error (block, "Method %s does not end with return or throw", m.name ());
		else
		    m.implicitVoidReturn (true);
	    }
	}
    }

    private State checkStatement (ParseTreeNode p) {
	if (p == null)
	    return State.NO_RETURN;
	if (p instanceof Block b) {
	    return endsWithReturnOrThrow (b.get ());
	}
	return endsWithReturnOrThrow (List.of (p));
    }

    private State endsWithReturnOrThrow (List<ParseTreeNode> ls) {
	int unreachableStart = -1;
	State hasReturnOrThrow = State.NO_RETURN;
	for (int i = 0, s = ls.size (); i < s; i++) {
	    ParseTreeNode p = ls.get (i);
	    if (hasReturnOrThrow == State.RETURNS) {
		error (p, "Unreachable code");
		unreachableStart = i;
		break;
	    }
	    if (hasReturnOrThrow == State.SOFT_RETURN) {
		warning (p, "Unreachable code");
		if (unreachableStart < 0)
		    unreachableStart = i;
	    }
	    switch (p) {
	    case ReturnStatement rs -> hasReturnOrThrow = State.RETURNS;
	    case ThrowStatement ts -> hasReturnOrThrow = State.RETURNS;
	    case BytecodeBlockBase bb -> hasReturnOrThrow = State.RETURNS;

	    case IfThenStatement ifts -> hasReturnOrThrow = handleIf (ifts);
	    case WhileStatement ws -> hasReturnOrThrow = handleWhile (ws);
	    case DoStatement ds -> hasReturnOrThrow = handleDo (ds);
	    case BasicForStatement f -> hasReturnOrThrow = handleBasicFor (f);
	    case EnhancedForStatement f -> hasReturnOrThrow = handleEnhancedFor (f);

	    case TryStatement t -> hasReturnOrThrow = handleTry (t);

	    case Block b -> hasReturnOrThrow = endsWithReturnOrThrow (b.get ());

	    case ExpressionStatement es -> handleIncrementDecrement (es);

	    default -> { /* empty */ }
	    }
	}
	if (unreachableStart > -1)
	    for (int i = ls.size () - 1; i >= unreachableStart; i--)
		ls.remove (i);
	return hasReturnOrThrow;
    }

    private State handleIf (IfThenStatement ifts) {
	handleStatementList (ifts.test ());
	State endsWithReturnOrThrow = checkStatement (ifts.thenPart ());
	ParseTreeNode ep = ifts.elsePart ();
	if (ep == null) {
	    // The JLS wants to allow if(DEBUG) { ... } <whatever> to not warn no matter
	    // if <whatever> contains code or not.
	    // This also means that we will require things that follow to have a return
	    // (but code generation may remove that).
	    if (IfGenerator.isTrue (ifts.test (), javaTokens))
		return State.SOFT_RETURN;
	} else {
	    State endsWithReturnOrThrowElse = checkStatement (ep);
	    return endsWithReturnOrThrow.and (endsWithReturnOrThrowElse);
	}

	return State.NO_RETURN;
    }

    private State handleWhile (WhileStatement ws) {
	handleStatementList (ws.expression ());
	checkStatement (ws.statement ());
	// TODO: investigate infinite loops
	return State.NO_RETURN;  // we do not know if it runs or not
    }

    private State handleDo (DoStatement ds) {
	handleStatementList (ds.expression ());
	checkStatement (ds.statement ());
	// TODO: investigate infinite loops
	return State.NO_RETURN;
    }

    private State handleBasicFor (BasicForStatement bfs) {
	handleStatementList (bfs.forInit ());
	handleStatementList (bfs.forUpdate ());
	checkStatement (bfs.statement ());

	if (isInfinite (bfs.expression ()))
	    return State.RETURNS; // TODO: do we want to have an infinite?
	return State.NO_RETURN;   // we do not know if it runs or not
    }

    private boolean isInfinite (ParseTreeNode p) {
	if (p == null)
	    return true;
	return p instanceof TokenNode tn && tn.token () == javaTokens.TRUE;
    }

    private void handleStatementList (ParseTreeNode p) {
	if (p instanceof StatementExpressionList sl)  {
	    List<ParseTreeNode> ls = sl.get ();
	    ls.forEach (this::handleIncrementDecrement);
	}
    }

    private State handleEnhancedFor (EnhancedForStatement efs) {
	checkStatement (efs.statement ());
	return State.NO_RETURN;
    }

    private State handleTry (TryStatement t) {
	handleStatementList (t.resources ());
	State blockEndsWithReturnOrThrow = checkStatement (t.block ());

	State allCatchReturnsOrThrows = State.RETURNS;
	Catches c = t.catches ();
	if (c != null) {
	    for (ParseTreeNode cs : c.get ()) {
		allCatchReturnsOrThrows = allCatchReturnsOrThrows.and (checkStatement (cs));
	    }
	}

	State finallyReturnsOrThrows = State.NO_RETURN;
	Finally fb = t.finallyBlock ();
	if (fb != null)
	    finallyReturnsOrThrows = checkStatement (fb.block ());

	State s1 = blockEndsWithReturnOrThrow.and (allCatchReturnsOrThrows);
	return s1.or (finallyReturnsOrThrows);
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
