package org.khelekore.parjac2.java11;

import java.nio.file.Path;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.khelekore.parjac2.java11.syntaxtree.TypeDeclaration;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;

    public BytecodeGenerator (Path origin, TypeDeclaration td) {
	this.origin = origin;
	this.td = td;
    }

    public byte[] generate () {
	ClassWriter cw = new ClassWriter (ClassWriter.COMPUTE_FRAMES);

	//        version,     access,             name,            signature, superName, interfaces
	cw.visit (Opcodes.V11, Opcodes.ACC_PUBLIC, td.getName (), null, "java/lang/Object", null);
	if (origin != null)
	    cw.visitSource (origin.getFileName ().toString (), null);

        cw.visitEnd ();
	return cw.toByteArray ();
    }
}