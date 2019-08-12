package org.khelekore.parjac2.parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.util.IntHolder;

public class Parser {

    private final boolean DEBUG = false;

    private final Grammar grammar;
    private final Path path;
    private final PredictCache predictCache;
    private final Lexer lexer;
    private final CompilerDiagnosticCollector diagnostics;

    private final IntHolder startPositions = new IntHolder (1024);
    private final List<Object> tokenValues = new ArrayList<> ();
    private final List<ParsePosition> parsePositions = new ArrayList<> ();

    // 2 ints per state: first is ruleid<<8 | dotPos, second is origin
    // Since rule ids are negative we shift (>>) down to keep id when we want rule
    private final IntHolder states = new IntHolder (4096);
    private final BitSet predictedRules;
    private final List<PredictGroup> predictions = new ArrayList<> ();

    // This one is reused every time we scan
    private final BitSet wantedScanTokens;

    /** We use this to try to avoid full scanning for duplicates.
     *  If we find a hash miss then we know there is no dup, if we have a hash collision
     *  We have to scan the current set to see.
     */
    private final BitSet hashOfStates;
    // TODO: how big? random prime for now, I tried 512 and that does not work well
    // I have only seen about ~150 states in one set
    private final int STATE_HASH_SIZE = 509;

    private int currentPosition = 0;

    public Parser (Grammar grammar, Path path, PredictCache predictCache, Lexer lexer,
		   CompilerDiagnosticCollector diagnostics) {
	this.grammar = grammar;
	this.path = path;
	this.predictCache = predictCache;
	this.lexer = lexer;
	this.diagnostics = diagnostics;
	startPositions.add (0);
	predictedRules = new BitSet (-grammar.getMaxRuleGroupId ());
	wantedScanTokens = new BitSet (grammar.getMaxTokenId ());
	hashOfStates = new BitSet (STATE_HASH_SIZE);
    }

    public void parse (Rule goalRule) {
	long startTime = System.currentTimeMillis ();
	addState (goalRule.getId (), 0, 0);

	while (lexer.hasMoreTokens ()) {
	    parsePositions.add (lexer.getParsePosition ());
	    int stateStartPos = startPositions.get (currentPosition);
	    if (DEBUG) {
		System.out.println ("currentPosition: " + currentPosition + ", states.size: " + states.size ());
		System.out.println ("stateStartPos: " + stateStartPos);
	    }
	    complete (stateStartPos);
	    predict ();
	    setupNextEarleyState ();
	    scan ();
	    tokenValues.add (lexer.getCurrentValue ());
	    // TODO: check for failures
	    if (isInError ()) {
		// TODO: try to recover
	    }
	    currentPosition++;
	}

	if (diagnostics.hasError ())
	    return;

	IntHolder goalHolder = new IntHolder (10);
	states.apply ((rp, o) -> findFinished (rp, o, goalRule, goalHolder),
		      startPositions.get (currentPosition), states.size ());
	if (goalHolder.size () < 2)
	    addParserError ("Did not find any finishing state");
	else if (goalHolder.size () > 2)
	    addParserError ("Found several valid parses: " + (goalHolder.size () / 2));

	if (diagnostics.hasError ())
	    return;

	long endTime = System.currentTimeMillis ();
	if (DEBUG)
	    // TODO: print on higher level than debug
	    System.out.println ("Successful parse of: " + path + " in " + (endTime - startTime) + " millis " +
				"states.size: " + states.size () + ", total tokens: " + currentPosition);

	try {
	    TreeInfo ti = generateParseTree (goalHolder.get (0), goalHolder.get (1), currentPosition, states.size ());
	    if (DEBUG)
		printTree (ti);
	    STNode root = ti.node;
	    if (root == null)
		addParserError ("Failed to generate parse tree for %s", path);
	} catch (Exception t) {
	    t.printStackTrace ();
	}
    }

    private void complete (int stateStartPos) {
	// No empty rules so we do not have to care about predictions, they are never completed.

	// if we added any state we may have to deal with the new ones
	int end;
	int start = stateStartPos;
	while (start < (end = states.size ())) {
	    states.apply (this::tryComplete, start, end);
	    start = end;
	}
    }

    private void tryComplete (int rulePos, int origin) {
	int dotPos = rulePos & 0xff;
	int rule = rulePos >> 8;
	Rule r = grammar.getRule (rule);
	if (r.size () == dotPos) {
	    completeLast (rulePos, origin, r);
	}
    }

    private void completeLast (int rulePos, int origin, Rule r) {
	int stateStartPos = startPositions.get (origin);
	int stateEndPos = origin < startPositions.size () ? startPositions.get (origin + 1) : states.size ();
	states.apply ((crp, co) -> tryAdvance (rulePos, origin, r, crp, co), stateStartPos, stateEndPos);
	PredictGroup pg = predictions.get (origin);
	IntHolder ih = pg.getRulesWithNext (r.getGroupId ());
	if (ih != null)
	    ih.apply (crp -> advancePrediction (rulePos, origin, crp), 0, ih.size ());
    }

    private void tryAdvance (int rulePos, int origin, Rule r, int cRulePos, int cOrigin) {
	int cDotPos = cRulePos & 0xff;
	int crule = cRulePos >> 8;
	Rule candidate = grammar.getRule (crule);
	if (cDotPos < candidate.size ()) {
	    int part = candidate.get (cDotPos);
	    if (grammar.isRule (part) && grammar.sameRule (r.getId (), part))
		addState (crule, cDotPos + 1, cOrigin);
	}
    }

    private void advancePrediction (int rulePos, int origin, int cRulePos) {
	// We already know next is matching
	int crule = cRulePos >> 8;
	addState (crule, 1, origin);
    }

    private void predict () {
	predictedRules.clear ();
	states.apply ((rp, o) -> addRules (rp, o),
		      startPositions.get (currentPosition), states.size ());
	PredictGroup pg = predictCache.getPredictedRules (predictedRules);
	predictions.add (pg);
    }

    private void addRules (int rulePos, int origin) {
	int dotPos = rulePos & 0xff;
	int rule = rulePos >> 8;
	Rule r = grammar.getRule (rule);
	if (r.size () == dotPos)
	    return;
	int ruleGroupId = r.get (dotPos);
	if (grammar.isRule (ruleGroupId)) {
	    predictedRules.set (-ruleGroupId);
	}
    }

    private void setupNextEarleyState () {
	wantedScanTokens.clear ();
	hashOfStates.clear ();
    }

    private void scan () {
	// Find tokens that we want to scan
	states.apply ((rp, o) -> addTokens (rp, wantedScanTokens),
		      startPositions.get (currentPosition), states.size ());
	PredictGroup pg = predictions.get (currentPosition);
	wantedScanTokens.or (pg.getWantedScanTokens ());

	Token scannedToken = scanToken (currentPosition, wantedScanTokens);

	// TODO: deal with wrong token back from scan.
	if (!wantedScanTokens.get (scannedToken.getId ())) {
	    addParserError ("Got unexpected token: '%s', expected one of: %s",
			    scannedToken.getName (),
			    wantedScanTokens.stream ().mapToObj (i -> "'" + grammar.getToken (i).getName () + "'")
			    .collect (java.util.stream.Collectors.joining (", ")));
	}

	// Advance the states that can be advanced by the scanned token
	startPositions.add (states.size ());
	states.apply ((rp, o) -> advance (rp, o, scannedToken),
		      startPositions.get (currentPosition), states.size ());
	IntHolder ih = pg.getRulesWithNext (scannedToken.getId ());
	if (ih != null)
	    ih.apply (rp -> advancePrediction (rp, currentPosition), 0, ih.size ());
    }

    private void addTokens (int rulePos, BitSet tokens) {
	int dotPos = rulePos & 0xff;
	int rule = rulePos >> 8;
	Rule r = grammar.getRule (rule);
	if (dotPos >= r.size ())
	    return;
	int id = r.get (dotPos);
	if (grammar.isToken (id)) {
	    tokens.set (id);
	}
    }

    private Token scanToken (int currentPosition, BitSet wantedTokens) {
	Token t = lexer.nextToken (wantedTokens);
	if (DEBUG) {
	    String sTokens =
		wantedTokens.stream ()
		.mapToObj (i -> grammar.getToken (i).toString ())
		.collect (java.util.stream.Collectors.joining (", "));
	    System.out.println ("scanToken: " + currentPosition + ", wantedTokens: "  + sTokens + ", got: " + t);
	}
	return t;
    }

    private void advance (int rulePos, int origin, Token scannedToken) {
	int dotPos = rulePos & 0xff;
	int rule = rulePos >> 8;
	Rule r = grammar.getRule (rule);
	if (dotPos >= r.size ())
	    return;
	if (!nextIsMatchingToken (r, dotPos, scannedToken))
	    return;
	addState (rule, dotPos + 1, origin);
    }

    private void advancePrediction (int rulePos, int origin) {
	// we already know that next is matching
	int rule = rulePos >> 8;
	addState (rule, 1, origin);
    }

    private void addState (int rule, int dotPos, int origin) {
	int arp = rule << 8 | dotPos;

	int bitPos = Math.abs ((arp ^ origin) % STATE_HASH_SIZE);
	if (hashOfStates.get (bitPos)) {
	    if (states.checkFor (arp, origin, startPositions.get (currentPosition), states.size ())) {
		System.out.println ("Dup found for: " + grammar.getRule (rule).toReadableString(grammar) +
				    ", dotPos: " + dotPos);
		printStates (currentPosition);
		return;
	    }
	}
	hashOfStates.set (bitPos, true);
	states.add (arp, origin);
	if (DEBUG) {
	    System.out.println ("added State: " + readableRule (rule) +
				", dotPos: " + dotPos + ", origin: " + origin);
	}
    }

    private String readableRule (int rule) {
	Rule r = grammar.getRule (rule);
	return r.toReadableString (grammar);
    }

    private boolean nextIsMatchingToken (Rule r, int dotPos, Token scannedToken) {
	int tokenId = r.get (dotPos);
	return tokenId == scannedToken.getId ();
    }

    private boolean isInError () {
	return false;
    }

    private void findFinished (int rulePos, int origin, Rule goalRule, IntHolder goalHolder) {
	int dotPos = rulePos & 0xff;
	int rule = rulePos >> 8;
	if (rule != goalRule.getId ())
	    return;
	if (origin != 0)
	    return;
	if (dotPos < goalRule.size ())
	    return;
	goalHolder.add (rulePos, origin);
    }

    private TreeInfo generateParseTree (int rulePos, int origin, int completedIn, int endPos) {
	// TODO: rewrite to a breath first search.
	// TODO: we may have several options for example SwitchBlockStatementGroup and SwitchLabel
	// TOOD: both start the same way
	int rule = rulePos >> 8;
	Rule r = grammar.getRule (rule);
	if (DEBUG) {
	    System.out.println ("generateParseTree: rule: " + r.toReadableString(grammar) +
				", origin: " + origin + ", completedIn: " + completedIn);
	    printStates (completedIn);
	}
	int usedTokens = 0;
	List<STNode> children = new ArrayList<> (r.size ());
	for (int i = r.size () - 1; i >= 0; i--) {
	    int tokenDiff = 0;
	    int p = r.get (i);
	    if (grammar.isToken (p)) {
		if (DEBUG)
		    System.out.println ("Accepting token: " + grammar.getToken (p));
		tokenDiff = 1;
		children.add (new STNode (grammar.getToken (p), getTokenValue (completedIn - 1), null));
	    } else {
		if (DEBUG)
		    System.out.println ("Trying to find rule: " + grammar.getRuleGroupName (p));
		int originLEQ = i == 0 ? origin : completedIn;
		int originGEQ = origin + i;
		int ihPos = findCompleted (p, completedIn, originLEQ, originGEQ, endPos);
		if (ihPos < 0) {
		    System.err.println ("Failed to find completed: " + grammar.getRuleGroupName (p) +
					" for: " + r.toReadableString (grammar));
		    addParserError ("Failed to find completed: %s for %s",
				    grammar.getRuleGroupName (p),
				    r.toReadableString (grammar));
		    return null;
		}
		TreeInfo ti = generateParseTree (states.get (ihPos), states.get (ihPos + 1), completedIn, ihPos);
		tokenDiff = ti.usedTokens;
		children.add (ti.node);
	    }
	    usedTokens += tokenDiff;
	    completedIn -= tokenDiff;
	    if (tokenDiff > 0)
		endPos = startPositions.get (completedIn + 1);
	}
	Collections.reverse (children);
	STNode st = new STNode (r, null, children); // TODO: data?
	return new TreeInfo (st, usedTokens);
    }

    private Object getTokenValue (int position) {
	return tokenValues.get (position);
    }

    private int findCompleted (int ruleGroup, int completedIn, int originLEQ, int originGEQ, int endPos) {
	int stateStartPos = startPositions.get (completedIn);
	int stateEndPos = endPos;
	if (DEBUG)
	    System.out.println ("Looking in " + completedIn + ": " + stateStartPos + " - " + stateEndPos);
	return states.reverseCheckFor ((rp, o) -> validComplete (rp, o, ruleGroup, originLEQ, originGEQ),
				       stateEndPos, stateStartPos);
    }

    private boolean validComplete (int rulePos, int origin, int ruleGroup, int originLEQ, int originGEQ) {
	int dotPos = rulePos & 0xff;
	int rule = rulePos >> 8;
	Rule candidate = grammar.getRule (rule);
	if (DEBUG)
	    System.out.println ("candidate: " + candidate.toReadableString (grammar) + ", " +
				dotPos + ", " + origin +
				" oleq: " + originLEQ + ", ogeq: " + originGEQ +
				" => " +
				(candidate.getGroupId () == ruleGroup &&
				 candidate.size () == dotPos &&
				 origin <= originLEQ && origin >= originGEQ));
	return candidate.getGroupId () == ruleGroup &&
	    candidate.size () == dotPos &&
	    origin <= originLEQ && origin >= originGEQ;
    }

    private void printStates (int position) {
	int startPos = startPositions.get (position);
	int endPos = position < currentPosition ? startPositions.get (position + 1) : states.size ();
	states.apply (this::printStateEntry, startPos, endPos);
    }

    private void printStateEntry (int rulePos, int origin) {
	int dotPos = rulePos & 0xff;
	int ruleId = rulePos >> 8;
	Rule rule = grammar.getRule (ruleId);
	System.out.println ("\trule: " + rule.toReadableString (grammar) + ": " + dotPos + ", origin: " + origin);
    }

    private void addParserError (String format, Object... args) {
	diagnostics.report (SourceDiagnostics.error (path, lexer.getParsePosition (), format, args));
    }

    private void printTree (TreeInfo ti) {
	printTree (ti.node, "");
    }

    private void printTree (STNode n, String indent) {
	System.out.print (indent);
	System.out.print (n.id);
	if (n.value != null)
	    System.out.print ("(" + n.value + ")");
	System.out.println ();
	if (n.children != null) {
	    n.children.forEach (c -> printTree (c, indent + " "));
	}
    }

    private static class TreeInfo {
	private final STNode node;
	private final int usedTokens;

	public TreeInfo (STNode node, int usedTokens) {
	    this.node = node;
	    this.usedTokens = usedTokens;
	}

	@Override public String toString () {
	    return "TreeInfo{" + node + "}";
	}
    }

    private class STNode {
	private final Object id;
	private final Object value;
	private final List<STNode> children;

	public STNode (Object id, Object value, List<STNode> children) {
	    this.id = id;
	    this.value = value;
	    this.children = children;
	}

	@Override public String toString () {
	    return "STNode{" + id + ", " + value +
		(children == null ? "" : (", children:\n" + children)) + "}";
	}
   }
}