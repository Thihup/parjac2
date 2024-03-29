package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;

public class FileSourceProvider implements SourceProvider {
    private final List<Path> srcDirs;
    private final Charset encoding;
    private List<DirAndPath> paths = null;

    public FileSourceProvider (List<Path> srcDirs, Charset encoding) {
	this.srcDirs = srcDirs;
	this.encoding = encoding;
    }

    @Override public void setup (CompilerDiagnosticCollector diagnostics) throws IOException {
	if (paths != null)
	    throw new IllegalStateException ("Can only call setup once");
	paths = new ArrayList<> ();
	srcDirs.stream ().forEach (p -> addFiles (p, diagnostics));
    }

    private void addFiles (Path p, CompilerDiagnosticCollector diagnostics) {
	if (!Files.isDirectory (p)) {
	    diagnostics.report (new NoSourceDiagnostics ("input srcdir: %s is not a directory", p));
	    return;
	}

	try {
	    paths.addAll (Files.walk (p, FileVisitOption.FOLLOW_LINKS).
			  filter (FileSourceProvider::isJavaFile).
			  map (f -> new DirAndPath (p, f)).
			  collect (Collectors.toList ()));
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to get files from: %s", p));
	}
    }

    private static boolean isJavaFile (Path p) {
	// javac only accept ".java", ".Java" gives "invalid flag...".
	return p.getFileName ().toString ().endsWith (".java");
    }

    @Override public Collection<DirAndPath> getSourcePaths () {
	return paths;
    }

    @Override public CharBuffer getInput (Path path) throws IOException {
	ByteBuffer buf = ByteBuffer.wrap (Files.readAllBytes (path));
	CharsetDecoder decoder = encoding.newDecoder ();
	decoder.onMalformedInput (CodingErrorAction.REPORT);
	decoder.onUnmappableCharacter (CodingErrorAction.REPORT);
	return decoder.decode (buf);
    }
}
