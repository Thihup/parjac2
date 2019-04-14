package org.khelekore.parjac2.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Grammar {

    /** Each rule has its own id.
     *  On the right hand side we have either token ids or rule group ids.
     */
    private List<Token> tokens = new ArrayList<> ();
    private Map<String, Token> tokenNameToToken = new HashMap<> ();
    private List<Rule> rules = new ArrayList<> ();
    private List<RuleGroup> ruleGroups = new ArrayList<> ();
    private Map<String, RuleGroup> nameToRuleGroup = new HashMap<> ();

    public final Token END_OF_INPUT;
    public final Token ERROR;

    public Grammar () {
	// skip zero element
	tokens.add (null);
	rules.add (null);
	ruleGroups.add (null);

	END_OF_INPUT = getToken ("end of input");
	ERROR = getToken ("Error");
    }

    public void validateRules () {
	for (int i = 1; i < rules.size (); i++)
	    if (rules.get (i).size () == 0)
		System.err.println ("Empty rule found: " + rules.get (i));
    }

    public Token getToken (String tokenName) {
	return tokenNameToToken.computeIfAbsent​ (tokenName, this::addToken);
    }

    private Token addToken (String tokenName) {
	Token t = new Token (tokenName, tokens.size ());
	tokens.add (t);
	return t;
    }

    public Rule addRule (String ruleName, int[] rightHandSide) {
	RuleGroup group = nameToRuleGroup.computeIfAbsent (ruleName, this::addRuleGroup);
	Rule r = new Rule (ruleName, -rules.size (), group.id, rightHandSide);
	rules.add (r);
	group.addRule (r);
	return r;
    }

    public Rule getRule (int id) {
	return rules.get (-id);
    }

    public Iterable<Rule> getRules () {
	return rules;
    }

    public boolean sameRule (int ruleId, int ruleGroupId) {
	Rule r = getRule (ruleId);
	return r.getGroupId () == ruleGroupId;
    }

    public int getRuleGroupId (String name) {
	RuleGroup rg = nameToRuleGroup.computeIfAbsent (name, this::addRuleGroup);
	return rg.id;
    }

    private RuleGroup addRuleGroup (String name) {
	RuleGroup g = new RuleGroup (name, -ruleGroups.size ());
	ruleGroups.add (g);
	return g;
    }

    public List<Rule> getRuleGroup (int ruleGroupId) {
	RuleGroup r = ruleGroups.get (-ruleGroupId);
	return r.rules;
    }

    public Token getToken (int id) {
	return tokens.get (id);
    }

    public int getNumberOfTokens () {
	return tokens.size () - 1;
    }

    public int getNumberOfRules () {
	return rules.size () - 1;
    }

    public boolean isToken (int id) {
	return id > 0;
    }

    public boolean isRule (int id) {
	return id < 0;
    }

    private static class RuleGroup {
	private final String name;
	private final int id;
	private final List<Rule> rules = new ArrayList<> ();

	public RuleGroup (String name, int id) {
	    this.name = name;
	    this.id = id;
	}

	public void addRule (Rule r) {
	    rules.add (r);
	}
    }
}