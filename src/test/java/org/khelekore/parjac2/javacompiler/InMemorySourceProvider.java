package org.khelekore.parjac2.javacompiler;

import java.nio.CharBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import org.khelekore.parjac2.CompilerDiagnosticCollector;

/** Class that can be used to provde source code from memory strings. */
public class InMemorySourceProvider implements SourceProvider {
    private String filename;
    private String input;

    @Override public void setup (CompilerDiagnosticCollector diagnostics) {
	// empty
    }

    @Override public Collection<DirAndPath> getSourcePaths () {
	return List.of (new DirAndPath (Paths.get ("src"), Paths.get (filename)));
    }

    @Override public CharBuffer getInput (Path path) {
	return CharBuffer.wrap (input);
    }

    public void input (String filename, String input) {
	this.filename = filename;
	this.input = input;
    }

    public void clean () {
	input = null;
	filename = null;
    }
}
