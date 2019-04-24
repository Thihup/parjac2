package org.khelekore.parjac2.parser;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

public class PredictCache {

    private final Grammar grammar;
    private final Map<BitSet, PredictGroup> cache = new ConcurrentHashMap<> ();

    public PredictCache (Grammar grammar) {
	this.grammar = grammar;
    }

    /**
     * @param rules each bit is a RuleGroup id.
     */
    public PredictGroup getPredictedRules (BitSet rules) {
	PredictGroup ret = cache.get (rules);
	if (ret == null) {
	    // Need to clone since rules is modifiable
	    BitSet k = (BitSet)rules.clone ();
	    ret = cache.computeIfAbsent (k, key -> compute (rules));
	}
	return ret;
    }

    private PredictGroup compute (BitSet rules) {
	BitSet seen = new BitSet ();
	BitSet predicted = new BitSet ();
	do {
	    rules.stream ().forEach (b -> addRules (b, rules, predicted, seen));
	} while (rules.length () > 0);

	return new PredictGroup (grammar, predicted);
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
