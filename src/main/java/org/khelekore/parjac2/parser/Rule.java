package org.khelekore.parjac2.parser;

import java.util.Arrays;

public class Rule {
    private final String ruleName;
    private final int ruleId;
    private final int ruleGroupId;
    private final int[] rightHandSide;

    public Rule (String ruleName, int ruleId, int ruleGroupId, int[] rightHandSide) {
	this.ruleName = ruleName;
	this.ruleId = ruleId;
	this.ruleGroupId = ruleGroupId;
	this.rightHandSide = rightHandSide;
    }

    public String getName () {
	return ruleName;
    }

    public int getId () {
	return ruleId;
    }

    public int getGroupId () {
	return ruleGroupId;
    }

    public int size () {
	return rightHandSide.length;
    }

    public int get (int position) {
	return rightHandSide[position];
    }

    @Override public String toString () {
	return "Rule{" + ruleName + ", " + ruleId + "/" + ruleGroupId + ": " + Arrays.toString (rightHandSide) + "}";
    }

    public String toReadableString (Grammar g) {
	StringBuilder sb = new StringBuilder ();
	sb.append ("Rule{" + ruleName + ", " + ruleId + "/" + ruleGroupId + ": [");
	boolean first = true;
	for (int i : rightHandSide) {
	    if (!first)
		sb.append (" ");
	    if (g.isToken (i)) {
		Token t = g.getToken (i);
		sb.append ("'");
		sb.append (t.getName ());
		sb.append ("'");
	    } else if (g.isRule (i)){
		String name = g.getRuleGroupName (i);
		sb.append (name);
	    }
	    first = false;
	}
	sb.append ("]}");
	return sb.toString ();
    }
}