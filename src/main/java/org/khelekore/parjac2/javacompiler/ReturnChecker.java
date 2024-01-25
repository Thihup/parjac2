package org.khelekore.parjac2.javacompiler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    // current loop or switch
    private static final String CURRENT_LOOP = "";

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
	    Map<String, ParseTreeNode> labels = new HashMap<> ();
	    State endsWithReturnOrThrow = endsWithReturnOrThrow (block.get (), labels);
	    if (endsWithReturnOrThrow != State.RETURNS) {
		if (returnRequired)
		    error (block, "Method %s does not end with return or throw", m.name ());
		else
		    m.implicitVoidReturn (true);
	    }
	}
    }

    private State checkStatement (ParseTreeNode p, Map<String, ParseTreeNode> labels) {
	if (p == null)
	    return State.NO_RETURN;
	if (p instanceof Block b) {
	    return endsWithReturnOrThrow (b.get (), labels);
	}
	return endsWithReturnOrThrow (List.of (p), labels);
    }

    private State endsWithReturnOrThrow (List<ParseTreeNode> ls, Map<String, ParseTreeNode> labels) {
	int unreachableStart = -1;
	State hasReturnOrThrow = State.NO_RETURN;
	for (int i = 0, s = ls.size (); i < s; i++) {
	    ParseTreeNode p = ls.get (i);
	    // Useful for debugging
	    //System.err.println ("Checking return for: " + p + ", " + p.getClass ().getName ());
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

	    case IfThenStatement ifts -> hasReturnOrThrow = handleIf (ifts, labels);

	    // loops
	    case WhileStatement ws -> hasReturnOrThrow = handleWhile (ws, labels);
	    case DoStatement ds -> hasReturnOrThrow = handleDo (ds, labels);
	    case BasicForStatement f -> hasReturnOrThrow = handleBasicFor (f, labels);
	    case EnhancedForStatement f -> hasReturnOrThrow = handleEnhancedFor (f, labels);

	    case SwitchExpressionOrStatement sw -> handleSwitch (sw, labels);
	    case LambdaExpression le -> handleLambdaExpression (le, labels);

	    case TryStatement t -> hasReturnOrThrow = handleTry (t, labels);

	    case Block b -> hasReturnOrThrow = endsWithReturnOrThrow (b.get (), labels);

	    case LabeledStatement label -> addLabel (label, labels);
	    case ContinueStatement cs -> checkContinue (cs, labels);
	    case BreakStatement bs -> checkBreak (bs, labels);

	    case ExpressionStatement es -> handleIncrementDecrement (es);


	    // This means that we do not look into variable initializer and similar.
	    // That is a problem for lambda expressions and similar
	    default -> {
		endsWithReturnOrThrow (p.getChildren (), labels);
	    }
	    }
	}
	if (unreachableStart > -1)
	    for (int i = ls.size () - 1; i >= unreachableStart; i--)
		ls.remove (i);
	return hasReturnOrThrow;
    }

    private State handleIf (IfThenStatement ifts, Map<String, ParseTreeNode> labels) {
	handleStatementList (ifts.test (), labels);
	State endsWithReturnOrThrow = checkStatement (ifts.thenPart (), labels);
	ParseTreeNode ep = ifts.elsePart ();
	if (ep == null) {
	    // The JLS wants to allow if(DEBUG) { ... } <whatever> to not warn no matter
	    // if <whatever> contains code or not.
	    // This also means that we will require things that follow to have a return
	    // (but code generation may remove that).
	    if (IfGenerator.isTrue (ifts.test (), javaTokens))
		return State.SOFT_RETURN;
	} else {
	    State endsWithReturnOrThrowElse = checkStatement (ep, labels);
	    return endsWithReturnOrThrow.and (endsWithReturnOrThrowElse);
	}

	return State.NO_RETURN;
    }

    private State handleWhile (WhileStatement ws, Map<String, ParseTreeNode> labels) {
	try (LabelPopper ac = pushLoop (ws, labels)) {
	    handleStatementList (ws.expression (), labels);
	    checkStatement (ws.statement (), labels);
	    // TODO: investigate infinite loops
	    return State.NO_RETURN;  // we do not know if it runs or not
	}
    }

    private State handleDo (DoStatement ds, Map<String, ParseTreeNode> labels) {
	try (LabelPopper ac = pushLoop (ds, labels)) {
	    handleStatementList (ds.expression (), labels);
	    checkStatement (ds.statement (), labels);
	    // TODO: investigate infinite loops
	    return State.NO_RETURN;
	}
    }

    private State handleBasicFor (BasicForStatement bfs, Map<String, ParseTreeNode> labels) {
	try (LabelPopper ac = pushLoop (bfs, labels)) {
	    handleStatementList (bfs.forInit (), labels);
	    handleStatementList (bfs.forUpdate (), labels);
	    checkStatement (bfs.statement (), labels);

	    if (isInfinite (bfs.expression ()))
		return State.RETURNS; // TODO: do we want to have an infinite?
	    return State.NO_RETURN;   // we do not know if it runs or not
	}
    }

    private boolean isInfinite (ParseTreeNode p) {
	if (p == null)
	    return true;
	return p instanceof TokenNode tn && tn.token () == javaTokens.TRUE;
    }

    private void handleStatementList (ParseTreeNode p, Map<String, ParseTreeNode> labels) {
	if (p instanceof StatementExpressionList sl)  {
	    List<ParseTreeNode> ls = sl.get ();
	    ls.forEach (this::handleIncrementDecrement);
	}
    }

    private State handleEnhancedFor (EnhancedForStatement efs, Map<String, ParseTreeNode> labels) {
	try (LabelPopper ac = pushLoop (efs, labels)) {
	    checkStatement (efs.statement (), labels);
	    return State.NO_RETURN;
	}
    }

    private State handleSwitch (SwitchExpressionOrStatement sw, Map<String, ParseTreeNode> labels) {
	try (LabelPopper ac = pushLoop (sw, labels)) {
	    return checkStatement (sw.block (), labels);
	}
    }

    private State handleLambdaExpression (LambdaExpression le, Map<String, ParseTreeNode> labels) {
	// The lambda body executes in another context, so new labels for it.
	return checkStatement (le.body (), new HashMap<> ());
    }

    private State handleTry (TryStatement t, Map<String, ParseTreeNode> labels) {
	handleStatementList (t.resources (), labels);
	State blockEndsWithReturnOrThrow = checkStatement (t.block (), labels);

	State allCatchReturnsOrThrows = State.RETURNS;
	Catches c = t.catches ();
	if (c != null) {
	    for (ParseTreeNode cs : c.get ()) {
		allCatchReturnsOrThrows = allCatchReturnsOrThrows.and (checkStatement (cs, labels));
	    }
	}

	State finallyReturnsOrThrows = State.NO_RETURN;
	Finally fb = t.finallyBlock ();
	if (fb != null)
	    finallyReturnsOrThrows = checkStatement (fb.block (), labels);

	State s1 = blockEndsWithReturnOrThrow.and (allCatchReturnsOrThrows);
	return s1.or (finallyReturnsOrThrows);
    }

    private void addLabel (LabeledStatement label, Map<String, ParseTreeNode> labels) {
	ParseTreeNode previous = labels.put (label.id (), label.statement ());
	if (previous != null)
	    warning (label, "Overwriting label %s", label.id ());
	checkStatement (label.statement (), labels);
    }

    private void checkContinue (ContinueStatement cs, Map<String, ParseTreeNode> labels) {
	String id = cs.id ();
	ParseTreeNode ps = id == null ? labels.get (CURRENT_LOOP) : labels.get (id);
	if (ps == null)
	    error (cs, "Label not found");
	else if (!isLoop (ps))
	    error (cs, "Continue need to go to a loop");
    }

    private void checkBreak (BreakStatement bs, Map<String, ParseTreeNode> labels) {
	String id = bs.id ();
	ParseTreeNode ps = id == null ? labels.get (CURRENT_LOOP) : labels.get (id);
	if (ps == null)
	    error (bs, "Label not found");
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

    private boolean isLoop (ParseTreeNode ps) {
	return switch (ps) {
	case BasicForStatement bs -> true;
	case EnhancedForStatement es -> true;
	case DoStatement ds -> true;
	case WhileStatement ws -> true;
	default -> false;
	};
    }

    private LabelPopper pushLoop (ParseTreeNode p, Map<String, ParseTreeNode> labels) {
	ParseTreeNode previous = labels.put (CURRENT_LOOP, p);
	return () -> labels.put (CURRENT_LOOP, previous);
    }

    private interface LabelPopper extends AutoCloseable {
	@Override public void close ();
    }
}
