package org.khelekore.parjac2.javacompiler;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** A bytecode writer that stores the compiled classes as byte[] in memory */
public class InMemoryBytecodeWriter implements BytecodeWriter {
    private Map<String, byte[]> classes = new HashMap<> ();

    @Override public void createDirectory (Path path) {
	// ignore, everything in memory
    }

    @Override public void write (String className, Path path, byte[] data) {
	classes.put (className, data);
    }

    public Map<String, byte[]> classes () {
	return classes;
    }

    public void clean () {
	classes.clear ();
    }
}
