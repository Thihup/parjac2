package org.khelekore.parjac2.java11;

import java.io.IOException;
import java.nio.CharBuffer;
import java.nio.file.Path;
import java.util.Collection;

import org.khelekore.parjac2.CompilerDiagnosticCollector;

public interface SourceProvider {
    /** Setup for use, will be called before other methods */
    void setup (CompilerDiagnosticCollector diagnostics) throws IOException;

    /** Get all the input paths */
    Collection<DirAndPath> getSourcePaths ();

    /** Get the source data for a given input path */
    CharBuffer getInput (Path path) throws IOException;
}