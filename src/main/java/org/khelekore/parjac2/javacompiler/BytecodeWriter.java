package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.nio.file.Path;

public interface BytecodeWriter {
    /** Create the output directory needed for some class */
    void createDirectory (Path path) throws IOException;

    /** Write the given bytecode
     * @param path the relative path
     * @param data the actual bytecode
     */
    void write (String className, Path path, byte[] data) throws IOException;
}
