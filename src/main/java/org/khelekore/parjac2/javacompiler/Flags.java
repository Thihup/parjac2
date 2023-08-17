package org.khelekore.parjac2.javacompiler;

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

    public static boolean isPrivate (int flag) {
	return (flag & ACC_PRIVATE) == ACC_PRIVATE;
    }

    public static boolean isProtected (int flag) {
	return (flag & ACC_PROTECTED) == ACC_PROTECTED;
    }
}

/*
    public static final int ACC_ABSTRACT = 1024;
    public static final int ACC_ANNOTATION = 8192;
    public static final int ACC_BRIDGE = 64;
    public static final int ACC_DEPRECATED = 131072;
    public static final int ACC_ENUM = 16384;
    public static final int ACC_FINAL = 16;
    public static final int ACC_INTERFACE = 512;
    public static final int ACC_MANDATED = 32768;
    public static final int ACC_MODULE = 32768;
    public static final int ACC_NATIVE = 256;
    public static final int ACC_OPEN = 32;
    public static final int ACC_PRIVATE = 2;
    public static final int ACC_PROTECTED = 4;
    public static final int ACC_PUBLIC = 1;
    public static final int ACC_RECORD = 65536;
    public static final int ACC_STATIC = 8;
    public static final int ACC_STATIC_PHASE = 64;
    public static final int ACC_STRICT = 2048;
    public static final int ACC_SUPER = 32;
    public static final int ACC_SYNCHRONIZED = 32;
    public static final int ACC_SYNTHETIC = 4096;
    public static final int ACC_TRANSIENT = 128;
    public static final int ACC_TRANSITIVE = 32;
    public static final int ACC_VARARGS = 128;
    public static final int ACC_VOLATILE = 64;
*/
