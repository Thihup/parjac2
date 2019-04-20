package org.khelekore.parjac2.parser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
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

    // 2 ints per state: first is ruleid<<8 | dotPos, second is origin
    // Since rule ids are negative we shift (>>) down to keep id when we want rule
    private final IntHolder states = new IntHolder (4096);

    private final BitSet predictedRules;
    private final List<IntHolder> predictions = new ArrayList<> ();

    private final BitSet wantedScanTokens;

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
    }

    public void parse (Rule goalRule) {
	long startTime = System.currentTimeMillis ();
	addState (goalRule.getId (), 0, 0);

	while (lexer.hasMoreTokens ()) {
	    int stateStartPos = startPositions.get (currentPosition);
	    if (DEBUG) {
		System.out.println ("currentPosition: " + currentPosition + ", states.size: " + states.size ());
		System.out.println ("stateStartPos: " + stateStartPos);
	    }
	    complete (stateStartPos);
	    predict ();
	    scan ();
	    // TODO: check for failures
	    if (isInError ()) {

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
	System.out.println ("Successful parse of: " + path + " in " + (endTime - startTime) + " millis " +
			    "states.size: " + states.size () + ", total tokens: " + currentPosition);
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
	IntHolder ih = predictions.get (origin);
	ih.apply (crp -> tryAdvance (rulePos, origin, r, crp, origin), 0, ih.size ());
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

    private void predict () {
	predictedRules.clear ();
	states.apply ((rp, o) -> addRules (rp, o),
		      startPositions.get (currentPosition), states.size ());
	IntHolder currentPredictions = predictCache.getPredictedRules (predictedRules);
	predictions.add (currentPredictions);
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

    private void scan () {
	// Find tokens that we want to scan
	wantedScanTokens.clear ();
	states.apply ((rp, o) -> addTokens (rp, wantedScanTokens),
		      startPositions.get (currentPosition), states.size ());
	IntHolder ih = predictions.get (currentPosition);
	ih.apply (r -> addTokens (r, wantedScanTokens), 0, ih.size ());

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
	ih.apply (r -> advance (r, currentPosition, scannedToken), 0, ih.size ());
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

    private void addState (int rule, int dotPos, int origin) {
	int arp = rule << 8 | dotPos;
	if (states.checkFor ((rp, o) -> checkDup (arp, origin, rp, o),
			     startPositions.get (currentPosition), states.size ()))
	    return;
	states.add (arp, origin);
	if (DEBUG) {
	    System.out.println ("added State: " + readableRule (rule) +
				", dotPos: " + dotPos + ", origin: " + origin);
	}
    }

    private boolean checkDup (int rp1, int o1, int rp2, int o2) {
	return rp1 == rp2 && o1 == o2;
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

    private void addParserError (String format, Object... args) {
	diagnostics.report (SourceDiagnostics.error (path, lexer.getParsePosition (), format, args));
    }
}