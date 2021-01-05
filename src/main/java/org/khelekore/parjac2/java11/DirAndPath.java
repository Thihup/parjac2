package org.khelekore.parjac2.java11;

import java.nio.file.Path;

public class DirAndPath {
    private final Path dir;
    private final Path file;

    public DirAndPath (Path dir, Path file) {
	this.dir = dir;
	this.file = file;
    }

    public Path getDir () {
	return dir;
    }

    public Path getFile () {
	return file;
    }

    public Path getRelativePath () {
	return dir.relativize (file);
    }
}