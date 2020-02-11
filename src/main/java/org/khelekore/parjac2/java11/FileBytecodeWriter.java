package org.khelekore.parjac2.java11;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileBytecodeWriter implements BytecodeWriter {
    private final Path outputDir;

    public FileBytecodeWriter (Path outputDir) {
	this.outputDir = outputDir;
    }

    @Override public void createDirectory (Path path) throws IOException {
	Files.createDirectories (getRealPath (path));
    }

    @Override public void write (Path path, byte[] data) throws IOException {
	Files.write (getRealPath (path), data);
    }

    private Path getRealPath (Path path) {
	return Paths.get (outputDir.toString (), path.toString ());
    }
}