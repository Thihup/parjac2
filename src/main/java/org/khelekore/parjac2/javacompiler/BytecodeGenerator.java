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
import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.MethodSignature;
import io.github.dmlloyd.classfile.Opcode;
import io.github.dmlloyd.classfile.Signature;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.attribute.InnerClassInfo;
import io.github.dmlloyd.classfile.attribute.InnerClassesAttribute;
import io.github.dmlloyd.classfile.attribute.SignatureAttribute;
import io.github.dmlloyd.classfile.attribute.SourceFileAttribute;

import org.khelekore.parjac2.javacompiler.syntaxtree.*;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;
    private final ClassInformationProvider cip;
    private final FullNameHandler name;
    private final JavaTokens javaTokens;

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
	this.javaTokens = javaTokens;

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
	return generateClass (ImplicitClassFlags.CLASS_FLAGS, signature,
			      c.getSuperClass (), c.getSuperInterfaces ());
    }

    private byte[] generateClass (EnumDeclaration e) {
	String signature = "Ljava/lang/Enum<L" + name.getSlashName () + ";>;";
	return generateClass (ImplicitClassFlags.ENUM_FLAGS, signature,
			      enumClassType, List.of ());
    }

    private byte[] generateClass (RecordDeclaration r) {
	String signature = getClassSignature (r.getTypeParameters (), r.getSuperClass (), r.getSuperInterfaces ());
	return generateClass (ImplicitClassFlags.RECORD_FLAGS, signature,
			      recordClassType, List.of ());
    }

    private byte[] generateInterface (NormalInterfaceDeclaration i) {
	String signature = getClassSignature (i.getTypeParameters (), null, i.getExtendsInterfaces ());
	return generateClass (ImplicitClassFlags.INTERFACE_FLAGS, signature,
			      objectClassType, i.getExtendsInterfaces ());
    }

    private byte[] generateInterface (AnnotationTypeDeclaration at) {
	//List<String> superInterfaces = List.of ("java.lang.annotation.Annotation");
	return generateClass (ImplicitClassFlags.ANNOTATION_FLAGS, null,
			      objectClassType, List.of ()); // TODO: use superInterfaces
    }

    private byte[] generateEnumConstant (EnumConstant ec) {
	// TODO: extend the enum
	EnumDeclaration ed = ec.getParent ();
	FullNameHandler parentName = cip.getFullName (ed);
	return generateClass (ImplicitClassFlags.ENUM_CONSTANT_FLAGS, null,
			      new ClassType (parentName), List.of ());
    }

    private byte[] generateAnonymousClass (UnqualifiedClassInstanceCreationExpression ac) {
	return generateClass (ImplicitClassFlags.ANONYMOUS_CLASS_FLAGS, null,
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

    private byte[] generateClass (ImplicitClassFlags icf, String signature, ClassType superType, List<ClassType> superInterfaces) {
	byte[] b = Classfile.of().build (ClassDesc.of (name.getFullDollarName ()), classBuilder -> {
		classBuilder.withVersion (Classfile.JAVA_21_VERSION, 0);  // possible minor: PREVIEW_MINOR_VERSION
		classBuilder.withFlags (td.flags () | icf.flags);
		classBuilder.withSuperclass (ClassDesc.of ((superType != null ? superType : objectClassType).getFullDollarName ()));
		addSuperInterfaces (classBuilder, superInterfaces);

		addFields (classBuilder, td);
		addConstructors (classBuilder, td);
		addMethods (classBuilder, td);
		addStaticBlocks (classBuilder, td);

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

		addInnerClassAttributes (classBuilder);
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
		ClassDesc desc = ClassDescUtils.getParseTreeClassDesc (type);
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
				createConstructorContents (cb, c, msh);
			    });
			if (msh.signature != null)
			    mb.with (SignatureAttribute.of (MethodSignature.parseFrom (msh.signature)));
		    });
	    });
    }

    private MethodSignatureHolder getMethodSignature (ConstructorDeclarationBase c) {
	return getMethodSignature (c.getTypeParameters (), c.getFormalParameterList (), VOID_RETURN);
    }

    private void createConstructorContents (CodeBuilder cb, ConstructorDeclarationBase cdb, MethodSignatureHolder msh) {
        ConstructorBody body = cdb.body ();
	List<ParseTreeNode> statements = body.statements ();
	boolean explicitInvocation = false;
	if (!statements.isEmpty ()) {
	    ParseTreeNode p = statements.get (0);
	    if (isSuperOrThis (p)) {
		handleStatements (cb, p);
		explicitInvocation = true;
		statements = statements.subList (1, statements.size ());
	    }
	}
	if (!explicitInvocation)
	    addImplicitSuper (cb, cdb);

	List<SyntaxTreeNode> initializers = td.getInstanceInitializers ();
	if (initializers != null) {
	    handleStatements (cb, initializers);
	}
	handleStatements (cb, statements);
	cb.return_ ();
    }

    private boolean isSuperOrThis (ParseTreeNode p) {
	return p instanceof ExplicitConstructorInvocation;
    }

    private void addImplicitSuper (CodeBuilder cb, ConstructorDeclarationBase cdb) {
	cb.lineNumber (cdb.position ().getLineNumber ()); // about what we want
	cb.aload (0);
	ClassDesc owner = ClassDescUtils.getClassDesc (td.getSuperClass ());
	cb.invokespecial (owner, ConstantDescs.INIT_NAME, MethodTypeDesc.ofDescriptor ("()V"));
    }

    private void addMethods (ClassBuilder classBuilder, TypeDeclaration td) {
	td.getMethods ().forEach (m -> {
		MethodSignatureHolder msh = getMethodSignature (m);
		int flags = m.flags ();
		TypeKind returnType = FullNameHelper.getTypeKind (m.result ());
		ParseTreeNode body = m.getMethodBody ();
		classBuilder.withMethod (m.name (), msh.desc, flags, mb -> {
			if (!m.isAbstract ()) {
			    mb.withCode (cb -> {
				    addMethodContent (cb, (Block)body, returnType);
				});
			}
			if (msh.signature != null)
			    mb.with (SignatureAttribute.of (MethodSignature.parseFrom (msh.signature)));
		    });
	    });
    }

    private void addStaticBlocks (ClassBuilder classBuilder, TypeDeclaration td) {
	List<SyntaxTreeNode> ls = td.getStaticInitializers ();
	if (ls.isEmpty ())
	    return;

	classBuilder.withMethod ("<clinit>", MethodTypeDesc.ofDescriptor ("()V"), Flags.ACC_STATIC, mb -> {
		for (SyntaxTreeNode si : ls)
		    mb.withCode (cb -> {
			    handleStatements (cb, si);
			    cb.return_ ();
			});
	    });
    }

    private void addMethodContent (CodeBuilder cb, Block body, TypeKind returnType) {
	List<ParseTreeNode> statements = body.get ();
	boolean needReturn = !endsWithReturn (statements);
	handleStatements (cb, statements);
	if (needReturn)
	    cb.returnInstruction (returnType);
    }

    private boolean endsWithReturn (List<ParseTreeNode> parts) {
	// TODO: handle more cases, for example if/else where both parts return
	return parts.size () > 0 && parts.get (parts.size () - 1) instanceof ReturnStatement;
    }

    private void handleStatements (CodeBuilder cb, ParseTreeNode statement) {
	handleStatements (cb, List.of (statement));
    }

    private void handleStatements (CodeBuilder cb, List<? extends ParseTreeNode> statements) {
	Deque<Object> partsToHandle = new ArrayDeque<> ();
	partsToHandle.addAll (statements);
	while (!partsToHandle.isEmpty ()) {
	    handleStatement (cb, td, partsToHandle, partsToHandle.removeFirst ());
	}
    }

    private void handleStatement (CodeBuilder cb, TypeDeclaration td, Deque<Object> partsToHandle, Object p) {
	//System.err.println ("looking at: " + p + ", " + p.getClass ().getName ());
	switch (p) {
	case Handler h -> h.run (cb);
	case ExpressionName e -> runParts (partsToHandle, e.replaced ());
	case FieldAccess fa -> fieldAccess (cb, fa);
	case MethodInvocation mi -> methodInvocation (cb, partsToHandle, mi);
	case ReturnStatement r -> handleReturn (cb, partsToHandle, r);
	case UnaryExpression u -> handleUnaryExpression (cb, partsToHandle, u);
	case TwoPartExpression tp -> handleTwoPartExpression (cb, partsToHandle, tp);
	case Ternary t -> handleTernary (cb, partsToHandle, t);
	case IfThenStatement ifts -> handleIf (cb, partsToHandle, ifts);
	case Assignment a -> handleAssignment (cb, partsToHandle, a);
	case LocalVariableDeclaration lv -> handleLocalVariables (cb, partsToHandle, lv);
	case PostIncrementExpression pie -> handlePostIncrement (cb, partsToHandle, pie);
	case StringLiteral l -> cb.ldc (l.getValue ());
	case IntLiteral i -> handleInt (cb, i);
	case DoubleLiteral d -> handleDouble (cb, d);
	case ThisPrimary t -> cb.aload (cb.receiverSlot ());
	case TokenNode t -> handleToken (cb, t);
	case ParseTreeNode n -> addChildren (partsToHandle, n);
	default -> throw new IllegalArgumentException ("Unknown type: " + p + ", " + p.getClass ().getName ());
	}
    }

    private interface Handler {
	void run (CodeBuilder cb);
    }

    private void fieldAccess (CodeBuilder cb, FieldAccess fa) {
	cb.lineNumber (fa.position ().getLineNumber ()); // should be good enough
	ParseTreeNode from = fa.from ();
	if (from != null) {
	    ClassDesc owner = ClassDesc.of (((ClassType)from).getFullDollarName ());
	    String name = fa.name ();
	    ClassDesc type = ClassDesc.of (fa.getFullName ().getFullDollarName ());
	    // Not correct, but works for: hello world!
	    cb.getstatic (owner, name, type);
	} else {
	    VariableInfo vi = fa.variableInfo ();
	    if (vi.fieldType () == VariableInfo.Type.PARAMETER) {
		loadParameter (cb, (FormalParameterBase)vi);
	    } else if (vi.fieldType () == VariableInfo.Type.LOCAL) {
		LocalVariable lv = (LocalVariable)vi;
		int slot = lv.vd ().slot ();
		FullNameHandler fn = FullNameHelper.type (lv.type ());
		TypeKind kind = FullNameHelper.getTypeKind (fn);
		cb.loadInstruction (kind, slot);
	    } else { // field
		getField (cb, vi);
	    }
	}
    }

    private void loadParameter (CodeBuilder cb, FormalParameterBase fpb) {
	int slot = cb.parameterSlot (fpb.slot ());
	FullNameHandler type = FullNameHelper.type (fpb.type ());
	if (type == FullNameHandler.INT || type == FullNameHandler.BOOLEAN)
	    cb.iload (slot);
	else if (type == FullNameHandler.LONG)
	    cb.lload (slot);
	else if (type == FullNameHandler.DOUBLE)
	    cb.dload (slot);
	else if (type == FullNameHandler.FLOAT)
	    cb.fload (slot);
	else  // TODO: more types
	    cb.aload (slot);
    }

    private void methodInvocation (CodeBuilder cb, Deque<Object> partsToHandle, MethodInvocation mi) {
	// TODO: need to deal with static
	ParseTreeNode on = mi.getOn ();
	if (on instanceof AmbiguousName an)
	    on = an.replaced ();
	if (on == null)
	    on = new ThisPrimary (mi);
	MethodInfo info = mi.info ();
	ClassDesc owner = info.ownerDesc ();
	String name = info.name ();
	MethodTypeDesc type = info.methodTypeDesc ();
	Handler h = b -> { b.lineNumber (mi.position ().getLineNumber ()); b.invokevirtual (owner, name, type); };
	partsToHandle.add (h);
	for (ParseTreeNode a : mi.getArguments ()) {
	    partsToHandle.addFirst (a);
	}

	partsToHandle.addFirst (on);
    }

    private void handleReturn (CodeBuilder cb, Deque<Object> partsToHandle, ReturnStatement r) {
	FullNameHandler fm = r.type ();
	TypeKind tkm = FullNameHelper.getTypeKind (fm);
	Handler h = b -> b.returnInstruction (tkm);
	partsToHandle.addFirst (h);
	ParseTreeNode p = r.expression ();
	FullNameHandler fr = p == null ? FullNameHandler.VOID : FullNameHelper.type (p);
	if (fr.getType () == FullNameHandler.Type.PRIMITIVE && !fm.equals (fr)) {
	    TypeKind tkr = FullNameHelper.getTypeKind (fr);
	    h = b -> addPrimitiveCast (cb, tkr, tkm);
	    partsToHandle.addFirst (h);
	}
	if (p != null)
	    partsToHandle.addFirst (p);
    }

    private void addPrimitiveCast (CodeBuilder cb, TypeKind from, TypeKind to) {
	cb.convertInstruction (from, to);
    }

    private void handleUnaryExpression (CodeBuilder cb, Deque<Object> partsToHandle, UnaryExpression u) {
	Token t = u.operator ();
	if (t == javaTokens.MINUS) {
	    ParseTreeNode exp = u.expression ();
	    if (exp instanceof IntLiteral il) {
		int value = il.getValue ();
		handleInt (cb, -value);
	    }
	}
    }

    private void handleTwoPartExpression (CodeBuilder cb, Deque<Object> partsToHandle, TwoPartExpression two) {
	Token t = two.token ();
	if (t == javaTokens.DOUBLE_EQUAL) {
	    if (two.part2 () instanceof IntLiteral il && il.intValue () == 0) {
		runParts (partsToHandle, two.part1 (), intEqualHandler (Opcode.IFEQ));
	    } else {
		runParts (partsToHandle, two.part1 (), two.part2 (), intEqualHandler (Opcode.IF_ICMPEQ));
	    }
	} else if (t == javaTokens.PLUS) {
	    FullNameHandler fnt = two.type ();
	    ParseTreeNode p1 = two.part1 ();
	    ParseTreeNode p2 = two.part2 ();
	    Handler h1 = c -> widen (c, fnt, p1);
	    Handler h2 = c -> widen (c, fnt, p2);
	    runParts (partsToHandle, p1, h1, p2, h2, plusHandler (two.type ()));
	}
    }

    private Handler intEqualHandler (Opcode opcode) {
	return c -> c.ifThenElse (opcode, b -> b.iconst_1 (), b -> b.iconst_0 ());
    }

    private void widen (CodeBuilder cb, FullNameHandler fn, ParseTreeNode p) {
	FullNameHandler pfn = FullNameHelper.type (p);
	if (pfn == fn)
	    return;

	// TODO: handle more casts
	if (pfn == FullNameHandler.INT) {
	    if (fn == FullNameHandler.DOUBLE)
		cb.i2d ();
	}

	if (pfn == FullNameHandler.LONG) {
	    if (fn == FullNameHandler.FLOAT)
		cb.l2f ();
	}
    }

    private Handler plusHandler (FullNameHandler fn) {
	if (fn == FullNameHandler.INT)
	    return c -> c.iadd ();
	if (fn == FullNameHandler.DOUBLE)
	    return c -> c.dadd ();
	if (fn == FullNameHandler.FLOAT)
	    return c -> c.fadd ();
	throw new IllegalStateException ("Unhandled type: " + fn);
    }

    private void handleTernary (CodeBuilder cb, Deque<Object> partsToHandle, Ternary t) {
	Handler h = c -> c.ifThenElse (x -> handleStatements (x, t.thenPart ()), x -> handleStatements (x, t.elsePart ()));
	runParts (partsToHandle, t.test (), h);
    }

    private void handleIf (CodeBuilder cb, Deque<Object> partsToHandle, IfThenStatement i) {
	Handler h;
	if (i.hasElse ()) {
	    h = c -> c.ifThenElse (x -> handleStatements (x, i.thenPart ()), x -> handleStatements (x, i.elsePart ()));
	} else {
	    h = c -> c.ifThen (x -> handleStatements (x, i.thenPart ()));
	}
	runParts (partsToHandle, i.test (), h);
    }

    private void handleAssignment (CodeBuilder cb, Deque<Object> partsToHandle, Assignment a) {
	ParseTreeNode p = a.lhs ();
	if (p instanceof DottedName dn)
	    p = dn.replaced ();
	if (p instanceof FieldAccess fa) {
	    // from, value, putField
	    // TODO: need to handle from better.
	    ParseTreeNode from = fa.from ();
	    VariableInfo vi = fa.variableInfo ();
	    if (from != null) {
		handleStatements (cb, from);
		putField (cb, vi, a.rhs ());
	    } else { // this or local or static field
		TypeKind kind = FullNameHelper.getTypeKind (FullNameHelper.type (vi.type ()));
		switch (vi.fieldType ()) {
		case VariableInfo.Type.FIELD ->
		    putField (cb, vi, a.rhs ());
		case VariableInfo.Type.PARAMETER ->
		    putInLocalSlot (cb, kind, ((FormalParameterBase)vi).slot (), a.rhs ());
		case VariableInfo.Type.LOCAL ->
		    putInLocalSlot (cb, kind, ((LocalVariable)vi).slot (), a.rhs ());
		}
	    }
	} else if (p instanceof ArrayAccess aa) {
	    // field, slot, value, arraystore
	    TypeKind kind = FullNameHelper.getTypeKind (FullNameHelper.type (p));
	    handleStatements (cb, List.of (aa.from (), aa.slot (), a.rhs ()));
	    cb.arrayStoreInstruction (kind);
	} else {
	    throw new IllegalStateException ("Unhandled assignment type: " + p + ", " + p.getClass ().getName () +
					     ", " + p.position ().toShortString ());
	}
    }

    private void putInLocalSlot (CodeBuilder cb, TypeKind kind, int slot, ParseTreeNode value) {
	handleStatements (cb, value);
	cb.storeInstruction (kind, slot);
    }

    private void handleLocalVariables (CodeBuilder cb, Deque<Object> partsToHandle, LocalVariableDeclaration lvs) {
	FullNameHandler fn = FullNameHelper.type (lvs.getType ());
	for (VariableDeclarator lv : lvs.getDeclarators ()) {
	    // TODO: what about arrays?
	    TypeKind kind = FullNameHelper.getTypeKind (fn);
	    int slot = cb.allocateLocal (kind);
	    lv.localSlot (slot);
	    if (lv.hasInitializer ()) {
		handleStatements (cb, lv.initializer ());
		cb.storeInstruction (kind, slot);
	    }
	}
    }

    private void handlePostIncrement (CodeBuilder cb, Deque<Object> partsToHandle, PostIncrementExpression pie) {
	ParseTreeNode tn = pie.expression ();
	if (tn instanceof DottedName dn)
	    tn = dn.replaced ();
	if (tn instanceof FieldAccess fa) {
	    ParseTreeNode from = fa.from ();
	    if (from != null) {
		// TODO: implement
	    } else {
		VariableInfo vi = fa.variableInfo ();
		switch (vi.fieldType ()) {
		case VariableInfo.Type.PARAMETER -> incrementLocalVariable (cb, ((FormalParameterBase)vi).slot (), 1);
		case VariableInfo.Type.LOCAL -> incrementLocalVariable (cb, ((LocalVariable)vi).slot (), 1);
		default -> incrementField (cb, vi);
		}
	    }
	} else if (tn instanceof ArrayAccess aa) {
	    // TODO: implement
	} else {
	    throw new IllegalStateException ("Unhandled post increment type: " + tn + ", " + tn.getClass ().getName () +
					     ", " + tn.position ().toShortString ());
	}
    }

    private void incrementLocalVariable (CodeBuilder cb, int slot, int value) {
	cb.incrementInstruction (slot, value);
    }

    private void incrementField (CodeBuilder cb, VariableInfo vi) {
	ClassDesc owner = ClassDescUtils.getClassDesc (cip.getFullName (td));
	ClassDesc type = vi.typeClassDesc ();
	if (isInstanceField (vi)) {
	    cb.aload (cb.receiverSlot ());
	    cb.dup ();
	}
	if (isInstanceField (vi))
	    cb.getfield (owner, vi.name (), type);
	else
	    cb.getstatic (owner, vi.name (), type);
	cb.iconst_1 ();
	cb.iadd ();
	if (isInstanceField (vi))
	    cb.putfield (owner, vi.name (), type);
	else
	    cb.putstatic (owner, vi.name (), type);
    }

    private void getField (CodeBuilder cb, VariableInfo vi) {
	ClassDesc owner = ClassDescUtils.getClassDesc (cip.getFullName (td));
	ClassDesc type = vi.typeClassDesc ();
	if (isInstanceField (vi)) {
	    cb.aload (cb.receiverSlot ());
	    cb.getfield (owner, vi.name (), type);
	} else {
	    cb.getstatic (owner, vi.name (), type);
	}
    }

    public void putField (CodeBuilder cb, VariableInfo vi, ParseTreeNode value) {
	Handler h = c -> handleStatements (c, value);
	putField (cb, vi, h);
    }

    private void putField (CodeBuilder cb, VariableInfo vi, Handler h) {
	ClassDesc owner = ClassDescUtils.getClassDesc (cip.getFullName (td));
	ClassDesc type = vi.typeClassDesc ();
	if (isInstanceField (vi))
	    cb.aload (cb.receiverSlot ());
	h.run (cb);
	if (isInstanceField (vi)) {
	    cb.putfield (owner, vi.name (), type);
	} else {
	    cb.putstatic (owner, vi.name (), type);
	}
    }

    private boolean isInstanceField (VariableInfo vi) {
	return !Flags.isStatic (vi.flags ());
    }

    private void handleInt (CodeBuilder cb, IntLiteral il) {
	int i = il.intValue ();
	handleInt (cb, i);
    }

    private void handleInt (CodeBuilder cb, int i) {
	if (i >= -1 && i <= 5) {
	    switch (i) {
	    case -1 -> cb.iconst_m1 ();
	    case 0 -> cb.iconst_0 ();
	    case 1 -> cb.iconst_1 ();
	    case 2 -> cb.iconst_2 ();
	    case 3 -> cb.iconst_3 ();
	    case 4 -> cb.iconst_4 ();
	    case 5 -> cb.iconst_5 ();
	    }
	} else if (i >= -128 && i <= 127) {
	    cb.bipush (i);
	} else if (i >= -32768 && i <= 32767) {
	    cb.sipush (i);
	} else {
	    cb.ldc (i);
	}
    }

    private void handleDouble (CodeBuilder cb, DoubleLiteral dl) {
	double d = dl.doubleValue ();
	if (d == 0) {
	    cb.dconst_0 ();
	} else if (d == 1) {
	    cb.dconst_1 ();
	} else {
	    cb.ldc (d);
	}
    }

    private void handleToken (CodeBuilder cb, TokenNode tn) {
	Token t = tn.token ();
	if (t == javaTokens.NULL)
	    cb.aconst_null ();
	else
	    throw new IllegalStateException ("Unhandled token type: " + t);
    }

    private void runParts (Deque<Object> partsToHandle, Object... parts) {
	for (int i = parts.length - 1; i >= 0; i--)
	    partsToHandle.addFirst (parts[i]);
    }

    private void addChildren (Deque<Object> partsToHandle, ParseTreeNode p) {
	List<ParseTreeNode> parts = p.getChildren ();
	for (int i = parts.size () - 1; i >= 0; i--)
	    partsToHandle.addFirst (parts.get (i));
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
		paramDescs.add (ClassDescUtils.getParseTreeClassDesc (p));
		sb.append (genericTypeHelper.getGenericType (p, cip, true));
	    }
	}
	sb.append (")");

	foundGenericTypes |= hasGenericType (result);
	ClassDesc returnDesc = ClassDescUtils.getParseTreeClassDesc (result);
	sb.append (genericTypeHelper.getGenericType (result, cip, true));

	MethodTypeDesc descriptor = MethodTypeDesc.of (returnDesc, paramDescs.toArray (new ClassDesc[paramDescs.size ()]));

	String signature = foundGenericTypes ? sb.toString () : null;
	return new MethodSignatureHolder (descriptor, signature);
    }

    private record MethodSignatureHolder (MethodTypeDesc desc, String signature) {
	// empty
    }

    /* We need to add all nested classes, not just the direct inner classes of tdt.
     * Here is how it may look for: class Inners { class Inner1 { class Inner1_Inner1 {} } class Inner2 }
     #21= #14 of #7;   // Inner2=class Inners$Inner2 of class Inners
     #22= #16 of #7;   // Inner1=class Inners$Inner1 of class Inners
     #23= #18 of #16;  // Inner1_Inner1=class Inners$Inner1$Inner1_Inner1 of class Inners$Inner1
    */
    private void addInnerClassAttributes (ClassBuilder classBuilder) {
	List<InnerClassInfo> innerClassInfos = new ArrayList<> ();
	Deque<TypeDeclaration> queue = new ArrayDeque<> ();
	queue.add (td);
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
	TypeDeclaration outer = td.getOuterClass ();
	TypeDeclaration inner = td;
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
