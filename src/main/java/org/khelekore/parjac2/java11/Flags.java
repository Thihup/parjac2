package org.khelekore.parjac2.java11;

import org.objectweb.asm.Opcodes;

public class Flags {

    public static final int ACC_ABSTRACT = Opcodes.ACC_ABSTRACT;
    public static final int ACC_FINAL = Opcodes.ACC_FINAL;
    public static final int ACC_NATIVE = Opcodes.ACC_NATIVE;
    public static final int ACC_PRIVATE = Opcodes.ACC_PRIVATE;
    public static final int ACC_PROTECTED = Opcodes.ACC_PROTECTED;
    public static final int ACC_PUBLIC = Opcodes.ACC_PUBLIC;
    public static final int ACC_STATIC = Opcodes.ACC_STATIC;
    public static final int ACC_STRICT = Opcodes.ACC_STRICT;
    public static final int ACC_SYNCHRONIZED = Opcodes.ACC_SYNCHRONIZED;

    // This one does not exist in Opcodes.
    public static final int ACC_DEFAULT = Opcodes.ACC_DEPRECATED << 1;
}