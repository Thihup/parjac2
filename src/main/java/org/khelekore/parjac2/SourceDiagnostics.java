package org.khelekore.parjac2;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;

import javax.tools.Diagnostic;

import org.khelekore.parjac2.parser.ParsePosition;

public class SourceDiagnostics implements Diagnostic<Path> {
    private final Diagnostic.Kind kind;
    private final Path path;
    private final ParsePosition parsePosition;
    private final String format;
    private final Object[] args;

    private SourceDiagnostics (Diagnostic.Kind kind, Path path, ParsePosition  parsePosition,
			       String format, Object... args) {
	this.kind = kind;
	this.path = path;
	this.parsePosition = parsePosition;
	this.format = format;
	this.args = args;
    }

    @Override
    public String toString () {
	return getClass ().getSimpleName () + "{" + kind + ", " + path + ", " +
	    parsePosition + ", " + format + ", " + Arrays.toString (args) + "}";
    }

    public static  SourceDiagnostics error (Path path, ParsePosition  parsePosition,
					    String format, Object... args) {
	return new SourceDiagnostics (Diagnostic.Kind.ERROR, path, parsePosition, format, args);
    }

    public static SourceDiagnostics warning (Path path, ParsePosition  parsePosition,
					     String format, Object... args) {
	return new SourceDiagnostics (Diagnostic.Kind.WARNING, path, parsePosition, format, args);
    }

    @Override public Diagnostic.Kind getKind () {
	return kind;
    }

    @Override public Path getSource () {
	return path;
    }

    @Override public long getPosition () {
	return parsePosition.getTokenStartPos ();
    }

    @Override public long getStartPosition () {
	return parsePosition.getTokenStartPos ();
    }

    @Override public long getEndPosition () {
	return parsePosition.getTokenEndPos ();
    }

    @Override public long getLineNumber () {
	return parsePosition != null ? parsePosition.getLineNumber () : -1;
    }

    @Override public long getColumnNumber () {
	return parsePosition != null ? parsePosition.getTokenColumn () : -1;
    }

    @Override public String getCode () {
	return null;
    }

    @Override public String getMessage (Locale locale) {
	String msg = String.format (format, args);
	return String.format ("%s:%d:%d: %s: %s", path.toString (),
			      getLineNumber (), getColumnNumber (), kind, msg);
    }
}
