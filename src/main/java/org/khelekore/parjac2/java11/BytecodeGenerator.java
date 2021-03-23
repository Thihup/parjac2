package org.khelekore.parjac2.java11;

import java.nio.file.Path;

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.khelekore.parjac2.java11.syntaxtree.*;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;
    private final String name;

    private enum ImplicitClassFlags {
	CLASS_FLAGS (Opcodes.ACC_SUPER),
	ENUM_FLAGS (Opcodes.ACC_FINAL | Opcodes.ACC_ENUM),
	INTERFACE_FLAGS (Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT),
	ANNOTATION_FLAGS (Opcodes.ACC_ANNOTATION | Opcodes.ACC_INTERFACE),
	ENUM_CONSTANT_FLAGS (Opcodes.ACC_FINAL),
	ANONYMOUS_CLASS_FLAGS (Opcodes.ACC_SUPER);

	private int flags;

	private ImplicitClassFlags (int flags) {
	    this.flags = flags;
	}
    }

    public BytecodeGenerator (Path origin, TypeDeclaration td, String name) {
	this.origin = origin;
	this.td = td;
	this.name = name;
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
	    return generateAnonymousClass ((UnqualifiedClassInstanceCreationExpression)td);
	} else if (c == EnumConstant.class) {
	    EnumConstant ec = (EnumConstant)td;
	    if (ec.hasBody ()) {
		return generateEnumConstant (ec);
	    }
	} else {
	    System.err.println ("Unhandled class: " + c.getName ());
	}
	return new byte[0];
    }

    private byte[] generateClass (NormalClassDeclaration c) {
	// TODO: set signature and interfaces
	return generateClass (c, ImplicitClassFlags.CLASS_FLAGS, null, "java/lang/Object", null);
    }

    private byte[] generateClass (EnumDeclaration e) {
	String signature = "Ljava/lang/Enum<L" + name + ";>;";
	return generateClass (e, ImplicitClassFlags.ENUM_FLAGS, signature, "java/lang/Enum", null);
    }

    private byte[] generateInterface (NormalInterfaceDeclaration i) {
	return generateClass (i, ImplicitClassFlags.INTERFACE_FLAGS, null, "java/lang/Object", null);
    }

    private byte[] generateInterface (AnnotationTypeDeclaration at) {
	String[] superInterfaces = new String[] { "java/lang/annotation/Annotation" };
	return generateClass (at, ImplicitClassFlags.ANNOTATION_FLAGS, null,
			      "java/lang/Object", superInterfaces);
    }

    private byte[] generateEnumConstant (EnumConstant ec) {
	// TODO: extend the enum
	return generateClass (ec, ImplicitClassFlags.ENUM_CONSTANT_FLAGS, null, "java/lang/Object", null);
    }

    private byte[] generateAnonymousClass (UnqualifiedClassInstanceCreationExpression ac) {
	return generateClass (ac, ImplicitClassFlags.ANONYMOUS_CLASS_FLAGS, null, "java/lang/Object", null);
    }

    private byte[] generateClass (TypeDeclaration tdt, ImplicitClassFlags icf,
				  String signature, String superType, String[] superInterfaces) {
	ClassWriter cw = new ClassWriter (ClassWriter.COMPUTE_FRAMES);
	int flags = td.getFlags () | icf.flags;
	cw.visit (Opcodes.V11, flags, name, signature, superType, superInterfaces);
	if (origin != null)
	    cw.visitSource (origin.getFileName ().toString (), null);
        cw.visitEnd ();
	return cw.toByteArray ();
    }
}