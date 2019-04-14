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

    // 2 ints per state: first is ruleid, second is positions: 24 bit startpos, 8 bit dotPos.
    private final IntHolder states = new IntHolder (1024);
    private final List<IntHolder> predictions = new ArrayList<> ();

    public Parser (Grammar grammar, Path path, PredictCache predictCache, Lexer lexer,
		   CompilerDiagnosticCollector diagnostics) {
	this.grammar = grammar;
	this.path = path;
	this.predictCache = predictCache;
	this.lexer = lexer;
	this.diagnostics = diagnostics;
	startPositions.add (0);
    }

    public void parse (Rule goalRule) {
	int currentPosition = 0;
	addState (goalRule.getId (), 0);

	while (lexer.hasMoreTokens ()) {
	    int stateStartPos = startPositions.get (currentPosition);
	    if (DEBUG) {
		System.out.println ("currentPosition: " + currentPosition + ", states.size: " + states.size ());
		System.out.println ("stateStartPos: " + stateStartPos);
	    }
	    complete (stateStartPos);
	    predict (currentPosition);
	    scan (currentPosition);
	    // TODO: check for failures
	    if (isInError ()) {

	    }
	    currentPosition++;
	}

	if (diagnostics.hasError ())
	    return;

	IntHolder goalHolder = new IntHolder (10);
	states.apply ((r, p) -> findFinished (r, p, goalRule, goalHolder),
		      startPositions.get (currentPosition), states.size ());
	if (goalHolder.size () < 2)
	    addParserError ("Did not find any finishing state");
	if (goalHolder.size () > 2)
	    addParserError ("Found several valid parses: " + (goalHolder.size () / 2));
    }

    private void complete (int stateStartPos) {
	// No empty rules so we do not have to care about predictions, they are never completed.

	// if we added any state we may have to deal with the new ones
	// TODO: keep track of seen states.
	int end;
	int start = stateStartPos;
	while (start < (end = states.size ())) {
	    states.apply (this::tryComplete, start, end);
	    start = end;
	}
    }

    private void tryComplete (int rule, int positions) {
	Rule r = grammar.getRule (rule);
	int dotPos = positions & 0xff;
	if (r.size () == dotPos) {
	    completeLast (rule, positions, r);
	}
    }

    private void completeLast (int rule, int positions, Rule r) {
	int startPos = positions >>> 8;
	int stateStartPos = startPositions.get (startPos);
	int stateEndPos = startPos < startPositions.size () ? startPositions.get (startPos + 1) : states.size ();
	states.apply ((cr, cp) -> tryAdvance (rule, positions, r, cr, cp), stateStartPos, stateEndPos);
	IntHolder ih = predictions.get (startPos);
	ih.apply (cr -> tryAdvance (rule, positions, r, cr, startPos << 8), 0, ih.size ());
    }

    private void tryAdvance (int rule, int position, Rule r, int crule, int cposition) {
	Rule candidate = grammar.getRule (crule);
	int cDotPos = cposition & 0xff;
	if (cDotPos < candidate.size ()) {
	    int part = candidate.get (cDotPos);
	    if (grammar.isRule (part) && grammar.sameRule (r.getId (), part)) {
		// TODO: do we need to check for duplicates?
		int nextPosition = cposition & 0xffffff00 | (cDotPos + 1);
		addState (crule, nextPosition);
	    }
	}
    }

    private void predict (int currentPosition) {
	BitSet ruleParts = new BitSet ();
	states.apply ((r, p) -> addRules (r, p, ruleParts),
		      startPositions.get (currentPosition), states.size ());
	IntHolder currentPredictions = predictCache.getPredictedRules (ruleParts);
	predictions.add (currentPredictions);
    }

    private void addRules (int rule, int positions, BitSet ruleParts) {
	Rule r = grammar.getRule (rule);
	int dotPos = positions & 0xff;
	if (r.size () == dotPos)
	    return;
	int ruleGroupId = r.get (dotPos);
	if (grammar.isRule (ruleGroupId))
	    ruleParts.set (-ruleGroupId);
    }

    private void scan (int currentPosition) {
	int positions = currentPosition << 8;
	// Find tokens that we want to scan
	BitSet tokens = new BitSet ();
	states.apply ((r, p) -> addTokens (r, p, tokens),
		      startPositions.get (currentPosition), states.size ());
	IntHolder ih = predictions.get (currentPosition);
	ih.apply (r -> addTokens (r, positions, tokens), 0, ih.size ());

	Token scannedToken = scanToken (currentPosition, tokens);

	// TODO: deal with wrong token back from scan.
	if (!tokens.get (scannedToken.getId ())) {
	    addParserError ("Got unexpected token: '" + scannedToken.getName () + "', expected one of: " +
			    tokens.stream ().mapToObj (i -> "'" + grammar.getToken (i).getName () + "'")
			    .collect (java.util.stream.Collectors.joining (", ")));
	}

	// Advance the states that can be advanced by the scanned token
	startPositions.add (states.size ());
	states.apply ((r, p) -> advance (r, p, scannedToken),
		      startPositions.get (currentPosition), states.size ());
	ih.apply (r -> advance (r, positions, scannedToken), 0, ih.size ());
    }

    private void addTokens (int rule, int positions, BitSet tokens) {
	Rule r = grammar.getRule (rule);
	int dotPos = positions & 0xff;
	if (dotPos >= r.size ())
	    return;
	int id = r.get (dotPos);
	if (grammar.isToken (id))
	    tokens.set (id);
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

    private void advance (int rule, int positions, Token scannedToken) {
	Rule r = grammar.getRule (rule);
	int dotPos = positions & 0xff;
	if (dotPos >= r.size ())
	    return;
	if (!nextIsMatchingToken (r, dotPos, scannedToken))
	    return;
	dotPos++;
	positions &= 0xffffff00;
	positions |= dotPos;
	addState (rule, positions);
    }

    private void addState (int rule, int positions) {
	states.add (rule, positions);
	if (DEBUG) {
	    int dotPos = positions & 0xff;
	    int origin = positions >>> 8;
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

    private void findFinished (int rule, int positions, Rule goalRule, IntHolder goalHolder) {
	if (rule != goalRule.getId ())
	    return;
	int dotPos = positions & 0xff;
	int origin = positions >>> 8;
	if (origin != 0)
	    return;
	if (dotPos < goalRule.size ())
	    return;
	goalHolder.add (rule, positions);
    }

    private void addParserError (String error) {
	diagnostics.report (SourceDiagnostics.error (path, lexer.getParsePosition (), error));
    }
}