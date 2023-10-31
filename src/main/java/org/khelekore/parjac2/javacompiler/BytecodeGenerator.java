package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import io.github.dmlloyd.classfile.ClassBuilder;
import io.github.dmlloyd.classfile.ClassSignature;
import io.github.dmlloyd.classfile.Classfile;
import io.github.dmlloyd.classfile.MethodSignature;
import io.github.dmlloyd.classfile.Signature;
import io.github.dmlloyd.classfile.attribute.InnerClassInfo;
import io.github.dmlloyd.classfile.attribute.InnerClassesAttribute;
import io.github.dmlloyd.classfile.attribute.SignatureAttribute;
import io.github.dmlloyd.classfile.attribute.SourceFileAttribute;

import org.khelekore.parjac2.javacompiler.syntaxtree.*;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;
    private final ClassInformationProvider cip;
    private final FullNameHandler name;

    private static final ClassType enumClassType = new ClassType (FullNameHandler.JL_ENUM);
    private static final ClassType recordClassType = new ClassType (FullNameHandler.JL_RECORD);
    private static final ClassType objectClassType = new ClassType (FullNameHandler.JL_OBJECT);

    private final GenericTypeHelper genericTypeHelper;

    private final TokenNode VOID_RETURN;

    private enum ImplicitClassFlags {
	CLASS_FLAGS (Classfile.ACC_SUPER),
	ENUM_FLAGS (Classfile.ACC_FINAL | Classfile.ACC_ENUM),
	RECORD_FLAGS (Classfile.ACC_FINAL | Classfile.ACC_SUPER),
	INTERFACE_FLAGS (Classfile.ACC_INTERFACE | Classfile.ACC_ABSTRACT),
	ANNOTATION_FLAGS (Classfile.ACC_ANNOTATION | Classfile.ACC_INTERFACE),
	ENUM_CONSTANT_FLAGS (Classfile.ACC_FINAL),
	ANONYMOUS_CLASS_FLAGS (Classfile.ACC_SUPER);

	private int flags;

	private ImplicitClassFlags (int flags) {
	    this.flags = flags;
	}
    }

    public BytecodeGenerator (Path origin, TypeDeclaration td, ClassInformationProvider cip, JavaTokens javaTokens) {
	this.origin = origin;
	this.td = td;
	this.cip = cip;
	this.name = cip.getFullName (td);

	genericTypeHelper = new GenericTypeHelper ();
	VOID_RETURN = new TokenNode (javaTokens.VOID, null);
    }

    public byte[] generate () {
	return switch (td) {
	case NormalClassDeclaration n -> generateClass (n);
	case EnumDeclaration e -> generateClass (e);
	case RecordDeclaration r -> generateClass (r);
	case NormalInterfaceDeclaration i -> generateInterface (i);
	case AnnotationTypeDeclaration a -> generateInterface (a);
	case UnqualifiedClassInstanceCreationExpression u -> generateAnonymousClass (u);
	case EnumConstant ec when ec.hasBody () -> generateEnumConstant (ec);
	default -> {
	    // TODO: handle this error!
	    throw new IllegalStateException ("BytecodeGenerator: Unhandled class: " + td.getClass ().getName ());
	}
	};
    }

    private byte[] generateClass (NormalClassDeclaration c) {
	String signature = getClassSignature (c.getTypeParameters (), c.getSuperClass (), c.getSuperInterfaces ());
	return generateClass (c, ImplicitClassFlags.CLASS_FLAGS, signature,
			      c.getSuperClass (), c.getSuperInterfaces ());
    }

    private byte[] generateClass (EnumDeclaration e) {
	String signature = "Ljava/lang/Enum<L" + name.getSlashName () + ";>;";
	return generateClass (e, ImplicitClassFlags.ENUM_FLAGS, signature,
			      enumClassType, List.of ());
    }

    private byte[] generateClass (RecordDeclaration r) {
	String signature = getClassSignature (r.getTypeParameters (), r.getSuperClass (), r.getSuperInterfaces ());
	return generateClass (r, ImplicitClassFlags.RECORD_FLAGS, signature,
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
	FullNameHandler parentName = cip.getFullName (ed);
	return generateClass (ec, ImplicitClassFlags.ENUM_CONSTANT_FLAGS, null,
			      new ClassType (parentName), List.of ());
    }

    private byte[] generateAnonymousClass (UnqualifiedClassInstanceCreationExpression ac) {
	return generateClass (ac, ImplicitClassFlags.ANONYMOUS_CLASS_FLAGS, null,
			      ac.getSuperClass (), List.of ());
    }

    // TODO: we only want to generate a signature if we have any generic thing to deal with.
    private String getClassSignature (TypeParameters tps, ClassType superClass, List<ClassType> superInterfaces) {
	if (tps != null || hasGenericType (superClass) || hasGenericType (superInterfaces)) {
	    StringBuilder sb = new StringBuilder ();
	    genericTypeHelper.appendTypeParametersSignature (sb, tps, cip, false);
	    if (superClass != null) {
		sb.append (genericTypeHelper.getGenericType (superClass, cip, false));
	    } else {
		sb.append ("Ljava/lang/Object;");
	    }
	    if (superInterfaces != null) {
		for (ClassType ct : superInterfaces)
		    sb.append (genericTypeHelper.getGenericType (ct, cip, false));
	    }
	    return sb.toString ();
	}
	return null;
    }

    private boolean hasGenericType (List<ClassType> ls) {
	return ls != null && ls.stream ().anyMatch (this::hasGenericType);
    }

    private boolean hasGenericType (ParseTreeNode p) {
	return p instanceof ClassType ct && hasGenericType (ct);
    }

    private boolean hasGenericType (ClassType ct) {
	return ct != null && (ct.getFullNameHandler ().hasGenericType () || ct.getTypeParameter () != null);
    }


    private byte[] generateClass (TypeDeclaration tdt, ImplicitClassFlags icf,
				  String signature, ClassType superType, List<ClassType> superInterfaces) {
	byte[] b = Classfile.of().build (ClassDesc.of (name.getFullDollarName ()), classBuilder -> {
		classBuilder.withVersion (Classfile.JAVA_21_VERSION, 0);  // possible minor: PREVIEW_MINOR_VERSION
		classBuilder.withFlags (td.flags () | icf.flags);
		classBuilder.withSuperclass (ClassDesc.of ((superType != null ? superType : objectClassType).getFullDollarName ()));
		addSuperInterfaces (classBuilder, superInterfaces);

		addFields (classBuilder, td);
		addConstructors (classBuilder, td);
		addMethods (classBuilder, td);

		/* TODO: check if we want to do this instead
		classBuilder.with (SignatureAttribute.of (ClassSignature.of (typeParameters,
									     superclassSignature,
									     superInterfaceSignatures)));
		*/
		if (signature != null) {
		    classBuilder.with (SignatureAttribute.of (ClassSignature.parseFrom (signature)));
		}
		if (origin != null) {
		    classBuilder.with (SourceFileAttribute.of (origin.getFileName ().toString ()));
		}
		// add nest host attribute: top level class

		// add inner class attributes
		addInnerClassAttributes (classBuilder, tdt);
	    });
	return b;
    }

    private void addSuperInterfaces (ClassBuilder classBuilder, List<ClassType> superInterfaces) {
	if (superInterfaces != null) {
	    List<ClassDesc> ls = new ArrayList<> ();
	    for (ClassType ct : superInterfaces) {
		ClassDesc cd = ClassDesc.of (ct.getFullDollarName ());
		ls.add (cd);
	    }
	    classBuilder.withInterfaceSymbols (ls);
	}
    }

    private void addFields (ClassBuilder classBuilder, TypeDeclaration td) {
	td.getFields ().forEach ((name, info) -> {
		ParseTreeNode type = info.type ();
		ClassDesc desc = getParseTreeClassDesc (type);
		int arrayRank = info.arrayRank ();
		if (arrayRank > 0)
		    desc = desc.arrayType (arrayRank);
		String signature = getGenericSignature (type);
		classBuilder.withField (name, desc, fb -> {
			fb.withFlags (info.flags ());
			if (signature != null)
			    fb.with (SignatureAttribute.of (Signature.parseFrom (signature)));
		    });
	    });
    }

    private String getGenericSignature (ParseTreeNode type) {
	if (type instanceof ClassType ct) {
	    TypeParameter tp = ct.getTypeParameter ();
	    if (tp != null) {
		return "T" + tp.getId () + ";";
	    }
	}
	return null;
    }

    private void addConstructors (ClassBuilder classBuilder, TypeDeclaration td) {
	td.getConstructors ().forEach (c -> {
		int flags = c.flags ();
		MethodSignatureHolder msh = getMethodSignature (c);
		classBuilder.withMethod (ConstantDescs.INIT_NAME, msh.desc, flags, mb -> {
			mb.withCode (cb -> {
				cb.lineNumber (c.position ().getLineNumber ()); // not correct, but at least somewhat close
				cb.aload (0);
				ClassDesc owner = getClassDesc (td.getSuperClass ());
				cb.invokespecial (owner, ConstantDescs.INIT_NAME, msh.desc);
				cb.return_ ();
			    });
			if (msh.signature != null)
			    mb.with (SignatureAttribute.of (MethodSignature.parseFrom (msh.signature)));
		    });
	    });
    }

    private MethodSignatureHolder getMethodSignature (ConstructorDeclarationBase c) {
	return getMethodSignature (c.getTypeParameters (), c.getFormalParameterList (), VOID_RETURN);
    }

    private void addMethods (ClassBuilder classBuilder, TypeDeclaration td) {
	td.getMethods ().forEach (m -> {
		MethodSignatureHolder msh = getMethodSignature (m);
		int flags = m.flags ();
		classBuilder.withMethod (m.name (), msh.desc, flags, mb -> {
			mb.withCode (cb -> {
				cb.lineNumber (m.position ().getLineNumber ()); // not correct, but at least somewhat close
				cb.return_ ();});
			if (msh.signature != null)
			    mb.with (SignatureAttribute.of (MethodSignature.parseFrom (msh.signature)));
		    });
	    });
    }

    private MethodSignatureHolder getMethodSignature (MethodDeclarationBase m) {
	return getMethodSignature (m.getTypeParameters (), m.getFormalParameterList (), m.getResult ());
    }

    private MethodSignatureHolder getMethodSignature (TypeParameters tps, FormalParameterList params, ParseTreeNode result) {
	StringBuilder sb = new StringBuilder ();
	boolean foundGenericTypes = false;
	if (tps != null) {
	    foundGenericTypes = true;
	    genericTypeHelper.appendTypeParametersSignature (sb, tps, cip, false);
	}

	List<ClassDesc> paramDescs = List.of ();
	sb.append ("(");
	if (params != null) {
	    paramDescs = new ArrayList<> (params.size ());
	    for (FormalParameterBase fp : params.getParameters ()) {
		ParseTreeNode p = fp.type ();
		foundGenericTypes |= hasGenericType (p);
		paramDescs.add (getParseTreeClassDesc (p));
		sb.append (genericTypeHelper.getGenericType (p, cip, true));
	    }
	}
	sb.append (")");

	foundGenericTypes |= hasGenericType (result);
	ClassDesc returnDesc = getParseTreeClassDesc (result);
	sb.append (genericTypeHelper.getGenericType (result, cip, true));

	MethodTypeDesc descriptor = MethodTypeDesc.of (returnDesc, paramDescs.toArray (new ClassDesc[paramDescs.size ()]));

	String signature = foundGenericTypes ? sb.toString () : null;
	return new MethodSignatureHolder (descriptor, signature);
    }

    private record MethodSignatureHolder (MethodTypeDesc desc, String signature) {
	// empty
    }

    private ClassDesc getParseTreeClassDesc (ParseTreeNode type) {
	ClassDesc desc = switch (type) {
	case TokenNode tn -> getClassDesc (tn);
	case ClassType ct -> getClassDesc (ct);
	case ArrayType at -> getClassDesc (at);
	default -> throw new IllegalStateException ("BytecodeGenerator: Unhandled field type: " + type.getClass ().getName () + ": " + type);
	};
	return desc;
    }

    private ClassDesc getClassDesc (TokenNode tn) {
	String descriptor = FullNameHandler.getPrimitive (tn.getToken ()).getSignature ();
	return ClassDesc.ofDescriptor (descriptor);
    }

    private ClassDesc getClassDesc (ClassType ct) {
	if (ct == null)
	    return ConstantDescs.CD_Object; // common for super classes to be null
	return ClassDesc.of (ct.getFullDollarName ());
    }

    private ClassDesc getClassDesc (ArrayType at) {
	ParseTreeNode type = at.getType ();
	ClassDesc base = getParseTreeClassDesc (type);
	Dims dims = at.getDims ();
	int rank = dims.rank ();
	return base.arrayType (rank);
    }

    /* We need to add all nested classes, not just the direct inner classes of tdt.
     * Here is how it may look for: class Inners { class Inner1 { class Inner1_Inner1 {} } class Inner2 }
     #21= #14 of #7;   // Inner2=class Inners$Inner2 of class Inners
     #22= #16 of #7;   // Inner1=class Inners$Inner1 of class Inners
     #23= #18 of #16;  // Inner1_Inner1=class Inners$Inner1$Inner1_Inner1 of class Inners$Inner1
    */
    private void addInnerClassAttributes (ClassBuilder classBuilder, TypeDeclaration tdt) {
	List<InnerClassInfo> innerClassInfos = new ArrayList<> ();
	Deque<TypeDeclaration> queue = new ArrayDeque<> ();
	queue.add (tdt);
	while (!queue.isEmpty ()) {
	    TypeDeclaration outer = queue.removeFirst ();
	    for (TypeDeclaration inner : outer.getInnerClasses ()) {
		innerClassInfos.add (getInnerClassInfo (outer, inner));
		queue.add (inner);
	    }
	}
	/* We need to add outer classes as well.
	 * For the Inner1_Inner1 above javac produces:
	 #20= #18 of #15;   // Inner1=class Inners$Inner1 of class Inners
	 #21= #7 of #18;    // Inner1_Inner1=class Inners$Inner1$Inner1_Inner1 of class Inners$Inner1
	*/
	TypeDeclaration outer = tdt.getOuterClass ();
	TypeDeclaration inner = tdt;
	while (outer != null) {
	    innerClassInfos.add (getInnerClassInfo (outer, inner));
	    inner = outer;
	    outer = outer.getOuterClass ();
	}

	if (!innerClassInfos.isEmpty ())
	    classBuilder.with (InnerClassesAttribute.of (innerClassInfos));
    }

    private InnerClassInfo getInnerClassInfo (TypeDeclaration outer, TypeDeclaration inner) {
	ClassDesc innerClass = ClassDesc.of (cip.getFullName (inner).getFullDollarName ());
	Optional<ClassDesc> outerClass = Optional.of (ClassDesc.of (cip.getFullName (outer).getFullDollarName ()));
	Optional<String> innerName = Optional.of (inner.getName ());
	int flags = inner.flags ();
	return InnerClassInfo.of (innerClass, outerClass, innerName, flags);
    }
}
