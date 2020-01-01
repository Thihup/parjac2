package org.khelekore.parjac2.java11;

import java.io.IOException;

import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.GrammarReader;
import org.khelekore.parjac2.parser.Rule;

public class JavaGrammarHelper {
    public static Rule readAndValidateRules (Grammar grammar, boolean debug) throws IOException {
	GrammarReader gr = new GrammarReader (grammar, debug);
	gr.read (JavaGrammarHelper.class.getResource ("/java11/java11.pj"));
	Rule goalRule = grammar.addRule ("GOAL", new int[]{grammar.getRuleGroupId ("CompilationUnit"),
							   grammar.END_OF_INPUT.getId ()});
	grammar.validateRules ();
	return goalRule;
    }
}
