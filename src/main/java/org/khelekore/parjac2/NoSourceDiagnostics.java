package org.khelekore.parjac2;

import java.nio.file.Path;
import java.util.Locale;
import javax.tools.Diagnostic;

public class NoSourceDiagnostics implements Diagnostic<Path> {
    private final String format;
    private final Object[] args;

    public NoSourceDiagnostics (String format, Object... args) {
	this.format = format;
	this.args = args;
    }

    @Override public Diagnostic.Kind getKind () {
	return Diagnostic.Kind.ERROR;
    }

    @Override public Path getSource () {
	return null;
    }

    @Override public long getPosition () {
	return Diagnostic.NOPOS;
    }

    @Override public long getStartPosition () {
	return Diagnostic.NOPOS;
    }

    @Override public long getEndPosition () {
	return Diagnostic.NOPOS;
    }

    @Override public long getLineNumber () {
	return Diagnostic.NOPOS;
    }

    @Override public long getColumnNumber () {
	return Diagnostic.NOPOS;
    }

    @Override public String getCode () {
	return null;
    }

    @Override public String getMessage (Locale locale) {
	return String.format (format, args);
    }
}