package org.khelekore.parjac2;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;

/** A collector of diagnostics that is thread safe and can say if there
 *  has been any errors reported.
 */
public class CompilerDiagnosticCollector implements DiagnosticListener<Path> {
    private final List<Diagnostic<? extends Path>> list =
	Collections.synchronizedList (new ArrayList<> ());

    // Flag is set in one step and checked when that step has been fully handled
    private volatile int errorCount;
    private volatile int warningCount;

    @Override public void report (Diagnostic<? extends Path> diagnostic) {
	if (diagnostic.getKind () == Diagnostic.Kind.ERROR)
	    errorCount++;
	if (diagnostic.getKind () == Diagnostic.Kind.WARNING)
	    warningCount++;
	list.add (diagnostic);
    }

    public void addAll (CompilerDiagnosticCollector other) {
	list.addAll (other.list);
	errorCount += other.errorCount;
	warningCount += other.warningCount;
    }

    public void removeAll (CompilerDiagnosticCollector other) {
	list.removeAll (other.list);
	errorCount -= other.errorCount;
	warningCount -= other.warningCount;
    }

    public int errorCount () {
	return errorCount;
    }

    public boolean hasError () {
	return errorCount > 0;
    }

    public int warningCount () {
	return warningCount;
    }

    public boolean hasWarning () {
	return warningCount > 0;
    }

    public Stream<Diagnostic<? extends Path>> getDiagnostics () {
	return list.stream ();
    }

    public void clear () {
	synchronized (list) {
	    list.clear ();
	    warningCount = 0;
	    errorCount = 0;
	}
    }
}
