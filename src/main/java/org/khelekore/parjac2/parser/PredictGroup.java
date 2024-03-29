package org.khelekore.parjac2.parser;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.function.IntConsumer;
import org.khelekore.parjac2.util.IntHolder;

public class PredictGroup {
    private BitSet wantedScanTokens;
    private Map<Integer, IntHolder> nextTokenOrRuleGroupToRules = new HashMap<> ();

    public PredictGroup (Grammar grammar, BitSet predicted) {
	wantedScanTokens = new BitSet (grammar.getMaxTokenId ());
	predicted.stream ().forEach (rule -> addRule (grammar, -rule));
    }

    private void addRule (Grammar grammar, int rule) {
	Rule r = grammar.getRule (rule);
	Integer nextTokenOrRuleGroup = r.get (0);
	// TODO: not sure about size, does not look like we get more than this for any java grammar
	IntHolder ih = nextTokenOrRuleGroupToRules.computeIfAbsent (nextTokenOrRuleGroup, k -> new IntHolder (16));
	ih.add (rule << 8); // rule and dotpos
	if (grammar.isToken (nextTokenOrRuleGroup))
	    wantedScanTokens.set (nextTokenOrRuleGroup);
    }

    public BitSet getWantedScanTokens () {
	return wantedScanTokens;
    }

    public void apply (int tokenOrRuleGroup, IntConsumer ic) {
	IntHolder ih = nextTokenOrRuleGroupToRules.get (tokenOrRuleGroup);
	if (ih != null)
	    ih.apply (ic, 0, ih.size ());
    }

    public void applyAll (IntConsumer ic) {
	nextTokenOrRuleGroupToRules.values ().forEach (ih -> ih.apply (ic, 0, ih.size ()));
    }
}
