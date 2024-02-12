package org.khelekore.parjac2.javacompiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.code.BytecodeBlockBase;
import org.khelekore.parjac2.javacompiler.code.IfGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.*;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

import io.github.dmlloyd.classfile.attribute.ExceptionsAttribute;
import io.github.dmlloyd.classfile.constantpool.ClassEntry;

public class ReturnChecker extends SemanticCheckerBase {

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
	// TODO: check constructors
    }

    private void checkMethod (MethodDeclarationBase m) {
	// ClassSetter have already checked that the returned values are fine, so we only check
	// if we have return or throw as needed
	FullNameHandler res = m.result ();
	boolean returnRequired = res != FullNameHandler.VOID;

	ParseTreeNode body = m.getMethodBody ();
	if (body instanceof Block block) {
	    Map<String, ParseTreeNode> labels = new HashMap<> ();
	    List<ThrowStatement> exceptions = new ArrayList<> ();
	    ReturnState endsWithReturnOrThrow = endsWithReturnOrThrow (block.get (), labels, exceptions);
	    if (!endsWithReturnOrThrow.endsOrThrows ()) {
		if (returnRequired)
		    error (block, "Method %s does not end with return or throw", m.name ());
		else
		    m.implicitVoidReturn (true);
	    }

	    if (!exceptions.isEmpty ()) {
		validateThrownExceptions (m.thrownTypes (), exceptions);
	    }
	}
    }

    private ReturnState checkStatement (ParseTreeNode p, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	if (p == null)
	    return ReturnState.NO_RETURN;
	if (p instanceof Block b) {
	    return endsWithReturnOrThrow (b.get (), labels, exceptions);
	}
	return endsWithReturnOrThrow (List.of (p), labels, exceptions);
    }

    private ReturnState endsWithReturnOrThrow (List<ParseTreeNode> ls,
					       Map<String, ParseTreeNode> labels,
					       List<ThrowStatement> exceptions) {
	int unreachableStart = -1;
	ReturnState hasReturnOrThrow = ReturnState.NO_RETURN;
	for (int i = 0, s = ls.size (); i < s; i++) {
	    ParseTreeNode p = ls.get (i);
	    // Useful for debugging
	    //System.err.println ("Checking return for: " + p + ", " + p.getClass ().getName ());
	    if (hasReturnOrThrow.endsOrThrows ()) {
		error (p, "Unreachable code");
		unreachableStart = i;
		break;
	    } else if (hasReturnOrThrow == ReturnState.SOFT_RETURN) {
		warning (p, "Unreachable code");
		if (unreachableStart < 0)
		    unreachableStart = i;
	    }
	    switch (p) {
	    case ReturnStatement rs -> hasReturnOrThrow = ReturnState.RETURNS;
	    case ThrowStatement ts -> hasReturnOrThrow = addThrows (ts, exceptions);
	    case BytecodeBlockBase bb -> hasReturnOrThrow = ReturnState.RETURNS;

	    case IfThenStatement ifts -> hasReturnOrThrow = handleIf (ifts, labels, exceptions);

	    // loops
	    case WhileStatement ws -> hasReturnOrThrow = handleWhile (ws, labels, exceptions);
	    case DoStatement ds -> hasReturnOrThrow = handleDo (ds, labels, exceptions);
	    case BasicForStatement f -> hasReturnOrThrow = handleBasicFor (f, labels, exceptions);
	    case EnhancedForStatement f -> hasReturnOrThrow = handleEnhancedFor (f, labels, exceptions);

	    case SwitchExpressionOrStatement sw -> handleSwitch (sw, labels, exceptions);
	    case LambdaExpression le -> handleLambdaExpression (le, labels, exceptions);

	    case TryStatement t -> hasReturnOrThrow = handleTry (t, labels, exceptions);

	    case Block b -> hasReturnOrThrow = endsWithReturnOrThrow (b.get (), labels, exceptions);

	    case LabeledStatement label -> addLabel (label, labels, exceptions);
	    case ContinueStatement cs -> checkContinue (cs, labels);
	    case BreakStatement bs -> checkBreak (bs, labels);

	    case ExpressionStatement es -> handleIncrementDecrement (es);


	    // This means that we do not look into variable initializer and similar.
	    // That is a problem for lambda expressions and similar
	    default -> {
		endsWithReturnOrThrow (p.getChildren (), labels, exceptions);
	    }
	    }
	}
	if (unreachableStart > -1)
	    for (int i = ls.size () - 1; i >= unreachableStart; i--)
		ls.remove (i);
	return hasReturnOrThrow;
    }

    private ReturnState addThrows (ThrowStatement ts, List<ThrowStatement> exceptions) {
	exceptions.add (ts);
	return ReturnState.THROWS;
    }

    private ReturnState handleIf (IfThenStatement ifts, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	handleStatementList (ifts.test (), labels);
	ReturnState endsWithReturnOrThrow = checkStatement (ifts.thenPart (), labels, exceptions);
	ParseTreeNode ep = ifts.elsePart ();
	if (ep == null) {
	    // The JLS wants to allow if(DEBUG) { ... } <whatever> to not warn no matter
	    // if <whatever> contains code or not.
	    // This also means that we will require things that follow to have a return
	    // (but code generation may remove that).
	    if (IfGenerator.isTrue (ifts.test (), javaTokens))
		return ReturnState.SOFT_RETURN;
	} else {
	    ReturnState endsWithReturnOrThrowElse = checkStatement (ep, labels, exceptions);
	    return endsWithReturnOrThrow.and (endsWithReturnOrThrowElse);
	}

	return ReturnState.NO_RETURN;
    }

    private ReturnState handleWhile (WhileStatement ws, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	try (LabelPopper ac = pushLoop (ws, labels)) {
	    handleStatementList (ws.expression (), labels);
	    checkStatement (ws.statement (), labels, exceptions);
	    // TODO: investigate infinite loops
	    return ReturnState.NO_RETURN;  // we do not know if it runs or not
	}
    }

    private ReturnState handleDo (DoStatement ds, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	try (LabelPopper ac = pushLoop (ds, labels)) {
	    handleStatementList (ds.expression (), labels);
	    checkStatement (ds.statement (), labels, exceptions);
	    // TODO: investigate infinite loops
	    return ReturnState.NO_RETURN;
	}
    }

    private ReturnState handleBasicFor (BasicForStatement bfs, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	try (LabelPopper ac = pushLoop (bfs, labels)) {
	    handleStatementList (bfs.forInit (), labels);
	    handleStatementList (bfs.forUpdate (), labels);
	    checkStatement (bfs.statement (), labels, exceptions);

	    if (isInfinite (bfs.expression ()))
		return ReturnState.RETURNS; // TODO: do we want to have an infinite?
	    return ReturnState.NO_RETURN;   // we do not know if it runs or not
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

    private ReturnState handleEnhancedFor (EnhancedForStatement efs, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	try (LabelPopper ac = pushLoop (efs, labels)) {
	    checkStatement (efs.statement (), labels, exceptions);
	    return ReturnState.NO_RETURN;
	}
    }

    private ReturnState handleSwitch (SwitchExpressionOrStatement sw, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	try (LabelPopper ac = pushLoop (sw, labels)) {
	    return checkStatement (sw.block (), labels, exceptions);
	}
    }

    private ReturnState handleLambdaExpression (LambdaExpression le, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	// The lambda body executes in another context, so new labels for it.
	List<ThrowStatement> thrownInLambda = new ArrayList<> ();
	ReturnState res = checkStatement (le.body (), new HashMap<> (), thrownInLambda);
	ExceptionsAttribute ea = le.methodInfo ().exceptions ();
	List<ClassType> allowed = ea == null ? List.of () : getAllowed (ea);
	validateThrownExceptions (allowed, thrownInLambda);

	return res;
    }

    private List<ClassType> getAllowed (ExceptionsAttribute ea) {
	List<ClassType> ret = new ArrayList<> ();
	List<ClassEntry> ls = ea.exceptions ();
	for (ClassEntry ce : ls) {
	    FullNameHandler fn = FullNameHandler.ofInternalName (ce.asInternalName ());
	    ret.add (new ClassType (fn));
	}
	return ret;
    }

    private ReturnState handleTry (TryStatement t, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	handleStatementList (t.resources (), labels);
	List<ThrowStatement> thrownInTry = new ArrayList<> ();
	ReturnState blockEndsWithReturnOrThrow = checkStatement (t.block (), labels, thrownInTry);
	t.blockReturnStatus (blockEndsWithReturnOrThrow);

	ReturnState allCatchReturnsOrThrows = ReturnState.RETURNS;
	Catches c = t.catches ();
	if (c != null) {
	    for (ParseTreeNode cs : c.get ()) {
		allCatchReturnsOrThrows = allCatchReturnsOrThrows.and (checkStatement (cs, labels, exceptions));
		for (int i = thrownInTry.size () - 1; i >= 0; i--) {
		    if (isCaught (thrownInTry.get (i), cs))
			thrownInTry.remove (i);
		}
	    }
	}
	exceptions.addAll (thrownInTry);

	ReturnState finallyReturnsOrThrows = ReturnState.NO_RETURN;
	Finally fb = t.finallyBlock ();
	if (fb != null)
	    finallyReturnsOrThrows = checkStatement (fb.block (), labels, exceptions);

	ReturnState s1 = blockEndsWithReturnOrThrow.and (allCatchReturnsOrThrows);
	return s1.or (finallyReturnsOrThrows);
    }

    private boolean isCaught (ThrowStatement ts, ParseTreeNode cs) {
	FullNameHandler t = FullNameHelper.type (ts);
	FullNameHandler c = FullNameHelper.type (((CatchClause)cs).firstType ());
	if (t.equals (c) || cip.isSuperClass (c, t, diagnostics))
	    return true;
	return false;
    }

    private void addLabel (LabeledStatement label, Map<String, ParseTreeNode> labels, List<ThrowStatement> exceptions) {
	ParseTreeNode previous = labels.put (label.id (), label.statement ());
	if (previous != null)
	    warning (label, "Overwriting label %s", label.id ());
	checkStatement (label.statement (), labels, exceptions);
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

    private void validateThrownExceptions (List<ClassType> allowed, List<ThrowStatement> thrown) {
	for (ThrowStatement ts : thrown) {
	    FullNameHandler fn = FullNameHelper.type (ts);
	    Set<FullNameHandler> allSupers = cip.getAllSuperTypesUnchecked (fn, diagnostics);
	    if (!isThrowable (allSupers)) {
		error (ts, "Can only throw subclasses of Throwable");
		continue;
	    }
	    List<FullNameHandler> allTypes = new ArrayList<> (allSupers.size () + 1);
	    allTypes.add (fn);
	    allTypes.addAll (allSupers);
	    if (!isRuntimeException (allTypes)) {
		validateThrowsContains (allowed, ts, allTypes);
	    }
	}
    }

    private boolean isThrowable (Set<FullNameHandler> allSupers) {
	return allSupers.contains (FullNameHandler.JL_THROWABLE);
    }

    private boolean isRuntimeException (List<FullNameHandler> allSupers) {
	return allSupers.contains (FullNameHandler.JL_RUNTIME_EXCEPTION);
    }

    private void validateThrowsContains (List<ClassType> allowed, ThrowStatement ts, List<FullNameHandler> allTypes) {
	if (allowed != null) {
	    Set<FullNameHandler> allowedNames = allowed.stream ().map (ct -> ct.fullName ()).collect (Collectors.toSet ());
	    for (FullNameHandler st : allTypes)
		if (allowedNames.contains (st))
		    return;
	}
	error (ts, "Exception not found in methods throws clause");
    }

    private interface LabelPopper extends AutoCloseable {
	@Override public void close ();
    }
}
