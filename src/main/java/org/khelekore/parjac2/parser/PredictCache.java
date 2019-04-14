package org.khelekore.parjac2.parser;

import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.khelekore.parjac2.util.IntHolder;

public class PredictCache {

    private final Grammar grammar;
    private final Map<BitSet, IntHolder> cache = new HashMap<> ();

    public PredictCache (Grammar grammar) {
	this.grammar = grammar;
    }

    /**
     * @param rules each bit is a RuleGroup id.
     */
    public IntHolder getPredictedRules (BitSet rules) {
	// TODO: thread safety
	return cache.computeIfAbsent (rules, this::compute);
    }

    private IntHolder compute (BitSet rules) {
	BitSet seen = new BitSet ();
	BitSet predicted = new BitSet ();
	do {
	    rules.stream ().forEach (b -> addRules (b, rules, predicted, seen));
	} while (rules.length () > 0);

	IntHolder ret = new IntHolder (rules.size ());
	predicted.stream ().forEach (i -> ret.add (-i));
	return ret;
    }

    private void addRules (int ruleGroupId, BitSet rules, BitSet predicted, BitSet seen) {
	List<Rule> ruleGroup = grammar.getRuleGroup (-ruleGroupId);
	ruleGroup.forEach (r -> addPrediction (r, rules, predicted, seen));
	rules.clear (ruleGroupId);
    }

    private void addPrediction (Rule rule, BitSet rules, BitSet predicted, BitSet seen) {
	predicted.set (-rule.getId ());
	int p = rule.get (0); // no zero length rules
	if (grammar.isRule (p)) {
	    int bit = -p;
	    if (!seen.get (bit)) {
		rules.set (bit);
		seen.set (bit);
	    }
	}
    }
}
