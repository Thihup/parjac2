package org.khelekore.parjac2.java11;

import java.nio.file.Path;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.khelekore.parjac2.java11.syntaxtree.*;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;

    public BytecodeGenerator (Path origin, TypeDeclaration td) {
	this.origin = origin;
	this.td = td;
    }

    public byte[] generate () {
	Class<?> c = td.getClass ();
	if (c == NormalClassDeclaration.class) {
	    return generateClass ((NormalClassDeclaration)td);
	} else if (c == EnumDeclaration.class) {
	    return generateClass ((EnumDeclaration)td);
	} else if (c == NormalInterfaceDeclaration.class) {
	    return generateInterface ((NormalInterfaceDeclaration)td);
	} else if (c == AnnotationTypeDeclaration.class) {
	    return generateInterface ((AnnotationTypeDeclaration)td);
	} else if (c == UnqualifiedClassInstanceCreationExpression.class) {
	} else if (c == EnumConstant.class) {
	} else {
	    System.err.println ("Unhandled class: " + c.getName ());
	}
	return new byte[0];
    }

    private byte[] generateClass (NormalClassDeclaration c) {
	ClassWriter cw = new ClassWriter (ClassWriter.COMPUTE_FRAMES);
	int flags = c.getFlags () | Opcodes.ACC_SUPER;
	cw.visit (Opcodes.V11, flags, td.getName (), null, "java/lang/Object", null);
	if (origin != null)
	    cw.visitSource (origin.getFileName ().toString (), null);
        cw.visitEnd ();
	return cw.toByteArray ();
    }

    private byte[] generateClass (EnumDeclaration e) {
	ClassWriter cw = new ClassWriter (ClassWriter.COMPUTE_FRAMES);
	int flags = e.getFlags () | Opcodes.ACC_FINAL | Opcodes.ACC_ENUM;
	String signature = "Ljava/lang/Enum<L" + e.getName () + ";>;";
	cw.visit (Opcodes.V11, flags, td.getName (), signature, "java/lang/Enum", null);
	if (origin != null)
	    cw.visitSource (origin.getFileName ().toString (), null);
        cw.visitEnd ();
	return cw.toByteArray ();
    }

    private byte[] generateInterface (NormalInterfaceDeclaration i) {
	ClassWriter cw = new ClassWriter (ClassWriter.COMPUTE_FRAMES);
	int flags = i.getFlags () | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT;
	cw.visit (Opcodes.V11, flags, i.getName (), null, "java/lang/Object", null);
	if (origin != null)
	    cw.visitSource (origin.getFileName ().toString (), null);
        cw.visitEnd ();
	return cw.toByteArray ();
    }

    private byte[] generateInterface (AnnotationTypeDeclaration at) {
	ClassWriter cw = new ClassWriter (ClassWriter.COMPUTE_FRAMES);
	int flags = at.getFlags () | Opcodes.ACC_ANNOTATION | Opcodes.ACC_INTERFACE;
	cw.visit (Opcodes.V11, flags, at.getName (), null, "java/lang/Object",
		  new String[] { "java/lang/annotation/Annotation" });
	if (origin != null)
	    cw.visitSource (origin.getFileName ().toString (), null);
        cw.visitEnd ();
	return cw.toByteArray ();
    }
}