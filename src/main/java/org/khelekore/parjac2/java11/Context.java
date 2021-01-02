package org.khelekore.parjac2.java11;

import java.nio.file.Path;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.ParsePosition;

public class Context {
    private final Java11Tokens java11Tokens;
    private final Grammar grammar;
    private final CompilerDiagnosticCollector diagnostics;
    private final Path path;

    public Context (Java11Tokens java11Tokens, Grammar grammar,
		    CompilerDiagnosticCollector diagnostics, Path path) {
	this.java11Tokens = java11Tokens;
	this.grammar = grammar;
	this.diagnostics = diagnostics;
	this.path = path;
    }

    public Java11Tokens getTokens () {
	return java11Tokens;
    }

    public Grammar getGrammar () {
	return grammar;
    }

    public CompilerDiagnosticCollector getDiagnostics () {
	return diagnostics;
    }

    public Path getPath () {
	return path;
    }

    public void error (ParsePosition pos, String format, Object... args) {
	diagnostics.report (SourceDiagnostics.error (path, pos, format, args));
    }

    public void warning (ParsePosition pos, String format, Object... args) {
	diagnostics.report (SourceDiagnostics.warning (path, pos, format, args));
    }
}
