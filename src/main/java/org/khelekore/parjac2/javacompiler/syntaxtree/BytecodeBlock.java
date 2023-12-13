package org.khelekore.parjac2.javacompiler.syntaxtree;

import io.github.dmlloyd.classfile.CodeBuilder;

public interface BytecodeBlock {
    void generate (CodeBuilder cb);
}
