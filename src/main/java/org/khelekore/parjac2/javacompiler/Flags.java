package org.khelekore.parjac2.javacompiler;

import io.github.dmlloyd.classfile.ClassFile;

public class Flags {

    public static final int ACC_ABSTRACT = ClassFile.ACC_ABSTRACT;
    public static final int ACC_FINAL = ClassFile.ACC_FINAL;
    public static final int ACC_NATIVE = ClassFile.ACC_NATIVE;
    public static final int ACC_PRIVATE = ClassFile.ACC_PRIVATE;
    public static final int ACC_PROTECTED = ClassFile.ACC_PROTECTED;
    public static final int ACC_PUBLIC = ClassFile.ACC_PUBLIC;
    public static final int ACC_STATIC = ClassFile.ACC_STATIC;
    public static final int ACC_STRICT = ClassFile.ACC_STRICT;
    public static final int ACC_SYNCHRONIZED = ClassFile.ACC_SYNCHRONIZED;
    public static final int ACC_INTERFACE = ClassFile.ACC_INTERFACE;
    public static final int ACC_ENUM = ClassFile.ACC_ENUM;
    public static final int ACC_VARARGS = ClassFile.ACC_VARARGS;
    public static final int ACC_SYNTHETIC = ClassFile.ACC_SYNTHETIC;
    public static final int ACC_VOLATILE = ClassFile.ACC_VOLATILE;

    // This one does not exist in ClassFile.
    public static final int ACC_DEFAULT = 262144;

    public static boolean isPrivate (int flag) {
	return (flag & ACC_PRIVATE) == ACC_PRIVATE;
    }

    public static boolean isProtected (int flag) {
	return (flag & ACC_PROTECTED) == ACC_PROTECTED;
    }

    public static boolean isPublic (int flag) {
	return (flag & ACC_PUBLIC) == ACC_PUBLIC;
    }

    public static boolean isStatic (int flag) {
	return (flag & ACC_STATIC) == ACC_STATIC;
    }

    public static boolean isAbstract (int flag) {
	return (flag & ACC_ABSTRACT) == ACC_ABSTRACT;
    }

    public static boolean isFinal (int flag) {
	return (flag & ACC_FINAL) == ACC_FINAL;
    }

    public static boolean isInterface (int flag) {
	return (flag & ACC_INTERFACE) == ACC_INTERFACE;
    }

    public static boolean isVarArgs (int flag) {
	return (flag & ACC_VARARGS) == ACC_VARARGS;
    }

    public static boolean isVolatile (int flag) {
	return (flag & ACC_VOLATILE) == ACC_VOLATILE;
    }

    public static boolean isNative (int flag) {
	return (flag & ACC_NATIVE) == ACC_NATIVE;
    }

    public static boolean isStrict (int flag) {
	return (flag & ACC_STRICT) == ACC_STRICT;
    }

    public static boolean isSynchronized (int flag) {
	return (flag & ACC_SYNCHRONIZED) == ACC_SYNCHRONIZED;
    }

    public static boolean isInstanceField (VariableInfo vi) {
	return !isStatic (vi.flags ());
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
*/
