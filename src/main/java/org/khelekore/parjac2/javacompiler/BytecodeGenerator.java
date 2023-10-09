package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.dmlloyd.classfile.Classfile;
import io.github.dmlloyd.classfile.attribute.SourceFileAttribute;

import org.khelekore.parjac2.javacompiler.syntaxtree.*;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;
    private final ClassInformationProvider cip;
    private final String name;

    private enum ImplicitClassFlags {
	CLASS_FLAGS (Classfile.ACC_SUPER),
	ENUM_FLAGS (Classfile.ACC_FINAL | Classfile.ACC_ENUM),
	RECORD_FLAGS (Classfile.ACC_FINAL),  // qwerty: there does not seem to be any: Classfile.ACC_RECORD
	INTERFACE_FLAGS (Classfile.ACC_INTERFACE | Classfile.ACC_ABSTRACT),
	ANNOTATION_FLAGS (Classfile.ACC_ANNOTATION | Classfile.ACC_INTERFACE),
	ENUM_CONSTANT_FLAGS (Classfile.ACC_FINAL),
	ANONYMOUS_CLASS_FLAGS (Classfile.ACC_SUPER);

	private int flags;

	private ImplicitClassFlags (int flags) {
	    this.flags = flags;
	}
    }

    public BytecodeGenerator (Path origin, TypeDeclaration td, ClassInformationProvider cip) {
	this.origin = origin;
	this.td = td;
	this.cip = cip;
	this.name = cip.getFullDollarClassName (td);
    }

    public byte[] generate () {
	Class<?> c = td.getClass ();
	if (c == NormalClassDeclaration.class) {
	    return generateClass ((NormalClassDeclaration)td);
	} else if (c == EnumDeclaration.class) {
	    return generateClass ((EnumDeclaration)td);
	} else if (c == RecordDeclaration.class) {
	    return generateClass ((RecordDeclaration)td);
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
	    // TODO: handle this error!
	    System.err.println ("Unhandled class: " + c.getName ());
	}
	return new byte[0];
    }

    private byte[] generateClass (NormalClassDeclaration c) {
	// TODO: set signature and interfaces
	return generateClass (c, ImplicitClassFlags.CLASS_FLAGS, null, "java.lang.Object", null);
    }

    private byte[] generateClass (EnumDeclaration e) {
	String signature = "Ljava/lang/Enum<L" + name + ";>;";
	return generateClass (e, ImplicitClassFlags.ENUM_FLAGS, signature, "java.lang.Enum", null);
    }

    private byte[] generateClass (RecordDeclaration e) {
	String signature = "Ljava/lang/Record";
	return generateClass (e, ImplicitClassFlags.RECORD_FLAGS, signature, "java.lang.Record", null);
    }

    private byte[] generateInterface (NormalInterfaceDeclaration i) {
	return generateClass (i, ImplicitClassFlags.INTERFACE_FLAGS, null, "java.lang.Object", null);
    }

    private byte[] generateInterface (AnnotationTypeDeclaration at) {
	String[] superInterfaces = new String[] { "java.lang.annotation.Annotation" };
	return generateClass (at, ImplicitClassFlags.ANNOTATION_FLAGS, null,
			      "java.lang.Object", superInterfaces);
    }

    private byte[] generateEnumConstant (EnumConstant ec) {
	// TODO: extend the enum
	EnumDeclaration ed = ec.getParent ();
	String parentName = cip.getFullDollarClassName (ed);
	return generateClass (ec, ImplicitClassFlags.ENUM_CONSTANT_FLAGS, null, parentName, null);
    }

    private byte[] generateAnonymousClass (UnqualifiedClassInstanceCreationExpression ac) {
	return generateClass (ac, ImplicitClassFlags.ANONYMOUS_CLASS_FLAGS, null, "java.lang.Object", null);
    }

    private byte[] generateClass (TypeDeclaration tdt, ImplicitClassFlags icf,
				  String signature, String superType, String[] superInterfaces) {
	byte[] b = Classfile.of().build (ClassDesc.of (name), classBuilder -> {
		classBuilder.withVersion (Classfile.JAVA_21_VERSION, 0);  // possible minor: PREVIEW_MINOR_VERSION
		classBuilder.withFlags (td.getFlags () | icf.flags);
		classBuilder.withSuperclass (ClassDesc.of (superType));
		if (superInterfaces != null) {
		    List<ClassDesc> ls = new ArrayList<> ();
		    for (String si : superInterfaces)
			ls.add (ClassDesc.of (si));
		}
		if (origin != null)
		    classBuilder.with (SourceFileAttribute.of (origin.getFileName ().toString ()));

		// add fields: withField
		// add methods: withMethod, withMethodBody
	    });
	return b;
    }
}
