package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.dmlloyd.classfile.ClassSignature;
import io.github.dmlloyd.classfile.Classfile;
import io.github.dmlloyd.classfile.attribute.SignatureAttribute;
import io.github.dmlloyd.classfile.attribute.SourceFileAttribute;

import org.khelekore.parjac2.javacompiler.syntaxtree.*;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;
    private final ClassInformationProvider cip;
    private final String name;

    private static final ClassType enumClassType = new ClassType ("java.lang.Enum");
    private static final ClassType recordClassType = new ClassType ("java.lang.Record");
    private static final ClassType objectClassType = new ClassType ("java.lang.Object");

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
	String signature = getClassSignature (c.getTypeParameters (), c.getSuperClass (), c.getSuperInterfaces ());
	return generateClass (c, ImplicitClassFlags.CLASS_FLAGS, signature,
			      c.getSuperClass (), c.getSuperInterfaces ());
    }

    private byte[] generateClass (EnumDeclaration e) {
	String signature = "Ljava/lang/Enum<L" + name + ";>;";
	return generateClass (e, ImplicitClassFlags.ENUM_FLAGS, signature,
			      enumClassType, List.of ());
    }

    private byte[] generateClass (RecordDeclaration e) {
	String signature = "Ljava/lang/Record;";
	return generateClass (e, ImplicitClassFlags.RECORD_FLAGS, signature,
			      recordClassType, List.of ());
    }

    private byte[] generateInterface (NormalInterfaceDeclaration i) {
	String signature = getClassSignature (i.getTypeParameters (), null, i.getExtendsInterfaces ());
	return generateClass (i, ImplicitClassFlags.INTERFACE_FLAGS, signature,
			      objectClassType, i.getExtendsInterfaces ());
    }

    private byte[] generateInterface (AnnotationTypeDeclaration at) {
	//List<String> superInterfaces = List.of ("java.lang.annotation.Annotation");
	return generateClass (at, ImplicitClassFlags.ANNOTATION_FLAGS, null,
			      objectClassType, List.of ()); // TODO: use superInterfaces
    }

    private byte[] generateEnumConstant (EnumConstant ec) {
	// TODO: extend the enum
	EnumDeclaration ed = ec.getParent ();
	String parentName = cip.getFullDollarClassName (ed);
	return generateClass (ec, ImplicitClassFlags.ENUM_CONSTANT_FLAGS, null,
			      new ClassType (parentName), List.of ());
    }

    private byte[] generateAnonymousClass (UnqualifiedClassInstanceCreationExpression ac) {
	return generateClass (ac, ImplicitClassFlags.ANONYMOUS_CLASS_FLAGS, null,
			      ac.getSuperType (), List.of ());
    }

    private String getClassSignature (TypeParameters tps, ClassType superClass, List<ClassType> superInterfaces) {
	StringBuilder sb = new StringBuilder ();
	appendTypeParameters (sb, tps);
	if (superClass != null) {
	    sb.append (GenericTypeHelper.getGenericType (superClass));
	} else {
	    sb.append ("Ljava/lang/Object;");
	}
	if (superInterfaces != null) {
	    for (ClassType ct : superInterfaces)
		sb.append (GenericTypeHelper.getGenericType (ct));
	}
	return sb.toString ();
    }

    private void appendTypeParameters (StringBuilder sb, TypeParameters tps) {
	if (tps == null)
	    return;
	sb.append ("<");
	for (TypeParameter tp : tps.get ()) {
	    sb.append (tp.getId ());
	    TypeBound b = tp.getTypeBound ();
	    if (b != null) {
		ClassType bt = b.getType ();
		sb.append (cip.isInterface (bt.getFullName ()) ? "::" : ":");
		sb.append (bt.getExpressionType ().getDescriptor ());
		List<ClassType> ls = b.getAdditionalBounds ();
		if (ls != null) {
		    for (ClassType ab : ls)
			sb.append (":").append (ab.getExpressionType ().getDescriptor ());
		}
	    } else {
		sb.append (":Ljava/lang/Object;");
	    }
	}
	sb.append (">");
    }

    private byte[] generateClass (TypeDeclaration tdt, ImplicitClassFlags icf,
				  String signature, ClassType superType, List<ClassType> superInterfaces) {
	byte[] b = Classfile.of().build (ClassDesc.of (name), classBuilder -> {
		classBuilder.withVersion (Classfile.JAVA_21_VERSION, 0);  // possible minor: PREVIEW_MINOR_VERSION
		classBuilder.withFlags (td.getFlags () | icf.flags);
		classBuilder.withSuperclass (ClassDesc.of ((superType != null ? superType : objectClassType).getFullName ()));
		if (superInterfaces != null) {
		    List<ClassDesc> ls = new ArrayList<> ();
		    for (ClassType ct : superInterfaces) {
			ClassDesc cd = ClassDesc.of (ct.getFullName ());
			ls.add (cd);
		    }
		    classBuilder.withInterfaceSymbols (ls);
		}
		// add fields: withField
		// add methods: withMethod, withMethodBody

		/* TODO: check if we want to do this instead
		classBuilder.with (SignatureAttribute.of (ClassSignature.of (typeParameters,
									     superclassSignature,
									     superInterfaceSignatures)));
		*/
		if (signature != null)
		    classBuilder.with (SignatureAttribute.of (ClassSignature.parseFrom (signature)));
		if (origin != null)
		    classBuilder.with (SourceFileAttribute.of (origin.getFileName ().toString ()));
		// add nest host attribute: top level class
		// add inner class attributes
	    });
	return b;
    }
}
