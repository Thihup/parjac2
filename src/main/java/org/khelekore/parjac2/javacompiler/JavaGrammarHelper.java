package org.khelekore.parjac2.javacompiler;

import java.io.IOException;

import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.GrammarReader;
import org.khelekore.parjac2.parser.Rule;

/** Helper class to read the grammar.
 */
public class JavaGrammarHelper {

    /** Read the rules and add the GOAL rule consisting of CompilationUnit and END_OF_INPUT.
     */
    public static Rule readAndValidateRules (Grammar grammar, boolean debug) throws IOException {
	readRules (grammar, debug);
	Rule goalRule = grammar.addRule ("GOAL", new int[]{grammar.getRuleGroupId ("CompilationUnit"),
							   grammar.END_OF_INPUT.getId ()});
	grammar.validateRules ();
	return goalRule;
    }

    /** Read the grammar file.
     */
    public static void readRules (Grammar grammar, boolean debug) throws IOException {
	GrammarReader gr = new GrammarReader (grammar, debug);
	gr.read (JavaGrammarHelper.class.getResource ("/java21/java21.pj"));
    }
}
