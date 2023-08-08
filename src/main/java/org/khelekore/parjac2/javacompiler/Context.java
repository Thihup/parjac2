package org.khelekore.parjac2.javacompiler;

import java.nio.file.Path;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;

public class Context {
    private final JavaTokens javaTokens;
    private final Grammar grammar;
    private final CompilerDiagnosticCollector diagnostics;
    private final DirAndPath dirAndPath;
    private final FlagConverter flagConverter;

    public Context (JavaTokens javaTokens, Grammar grammar,
		    CompilerDiagnosticCollector diagnostics, DirAndPath dirAndPath,
		    FlagConverter flagConverter) {
	this.javaTokens = javaTokens;
	this.grammar = grammar;
	this.diagnostics = diagnostics;
	this.dirAndPath = dirAndPath;
	this.flagConverter = flagConverter;
    }

    public JavaTokens getTokens () {
	return javaTokens;
    }

    public Grammar getGrammar () {
	return grammar;
    }

    public CompilerDiagnosticCollector getDiagnostics () {
	return diagnostics;
    }

    public Path getPath () {
	return dirAndPath.getFile ();
    }

    public Path getRelativePath () {
	return dirAndPath.getRelativePath ();
    }

    public int getFlagValue (Token t) {
	return flagConverter.getValue (t);
    }

    public Token getToken (int value) {
	return flagConverter.getToken (value);
    }

    public String getTokenNameString (int mask) {
	return flagConverter.getTokenNameString (mask);
    }

    public void error (ParsePosition pos, String format, Object... args) {
	diagnostics.report (SourceDiagnostics.error (dirAndPath.getFile (), pos, format, args));
    }

    public void warning (ParsePosition pos, String format, Object... args) {
	diagnostics.report (SourceDiagnostics.warning (dirAndPath.getFile (), pos, format, args));
    }
}
