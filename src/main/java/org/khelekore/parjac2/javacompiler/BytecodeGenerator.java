package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import io.github.dmlloyd.classfile.ClassBuilder;
import io.github.dmlloyd.classfile.ClassSignature;
import io.github.dmlloyd.classfile.Classfile;
import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.MethodSignature;
import io.github.dmlloyd.classfile.Opcode;
import io.github.dmlloyd.classfile.Signature;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.attribute.InnerClassInfo;
import io.github.dmlloyd.classfile.attribute.InnerClassesAttribute;
import io.github.dmlloyd.classfile.attribute.SignatureAttribute;
import io.github.dmlloyd.classfile.attribute.SourceFileAttribute;

import org.khelekore.parjac2.javacompiler.syntaxtree.*;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class BytecodeGenerator {
    private final Path origin;
    private final TypeDeclaration td;
    private final ClassInformationProvider cip;
    private final FullNameHandler name;
    private final JavaTokens javaTokens;
    private final Grammar grammar;
    private final GenericTypeHelper genericTypeHelper;
    private final TokenNode VOID_RETURN;
    private final Map<FullNameHandler, Map<Token, Consumer<CodeBuilder>>> mathOps;
    private final Map<String, AtomicInteger> lambdaNames = new HashMap<> ();

    private static final ClassType enumClassType = new ClassType (FullNameHandler.JL_ENUM);
    private static final ClassType recordClassType = new ClassType (FullNameHandler.JL_RECORD);
    private static final ClassType objectClassType = new ClassType (FullNameHandler.JL_OBJECT);

    private static final String STATIC_INIT = "<clinit>";
    private static final String INSTANCE_INIT = "<init>";
    private static final MethodTypeDesc INIT_SIGNATURE = MethodTypeDesc.ofDescriptor ("()V");

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

    public BytecodeGenerator (Path origin, TypeDeclaration td, ClassInformationProvider cip, JavaTokens javaTokens, Grammar grammar) {
	this.origin = origin;
	this.td = td;
	this.cip = cip;
	this.name = cip.getFullName (td);
	this.javaTokens = javaTokens;
	this.grammar = grammar;

	genericTypeHelper = new GenericTypeHelper ();
	VOID_RETURN = new TokenNode (javaTokens.VOID, null);

	mathOps = Map.of (FullNameHandler.INT, TwoPartOperationsUtils.getIntMap (javaTokens),
			  FullNameHandler.LONG, TwoPartOperationsUtils.getLongMap (javaTokens),
			  FullNameHandler.DOUBLE, TwoPartOperationsUtils.getDoubleMap (javaTokens),
			  FullNameHandler.FLOAT, TwoPartOperationsUtils.getFloatMap (javaTokens)
		     );
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
	return ct != null && (ct.fullName ().hasGenericType () || ct.getTypeParameter () != null);
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
				MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, INSTANCE_INIT);
				mcb.createConstructorContents (cb, c, msh);
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
		TypeKind returnType = FullNameHelper.getTypeKind (m.result ());
		ParseTreeNode body = m.getMethodBody ();
		classBuilder.withMethod (m.name (), msh.desc, flags, mb -> {
			if (!m.isAbstract ()) {
			    mb.withCode (cb -> {
				    MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, m.name ());
				    mcb.addMethodContent (cb, (Block)body, returnType);
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

	classBuilder.withMethod (STATIC_INIT, INIT_SIGNATURE, Flags.ACC_STATIC, mb -> {
		for (SyntaxTreeNode si : ls)
		    mb.withCode (cb -> {
			    MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, STATIC_INIT);
			    mcb.handleStatements (cb, si);
			    cb.return_ ();
			});
	    });
    }

    private class MethodContentBuilder {
	private final ClassBuilder classBuilder;
	private final String methodName;

	public MethodContentBuilder (ClassBuilder classBuilder, String methodName) {
	    this.classBuilder = classBuilder;
	    this.methodName = methodName;
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
	    case MethodInvocation mi -> methodInvocation (cb, mi);
	    case ReturnStatement r -> handleReturn (cb, r);
	    case UnaryExpression u -> handleUnaryExpression (cb, u);
	    case TwoPartExpression tp -> handleTwoPartExpression (cb, tp);
	    case Ternary t -> handleTernary (cb, partsToHandle, t);
	    case IfThenStatement ifts -> handleIf (cb, partsToHandle, ifts);
	    case Assignment a -> handleAssignment (cb, partsToHandle, a);
	    case LocalVariableDeclaration lv -> handleLocalVariables (cb, lv);
	    case PostIncrementExpression pie -> handlePostIncrement (cb, partsToHandle, pie);
	    case PostDecrementExpression pde -> handlePostDecrement (cb, partsToHandle, pde);
	    case BasicForStatement bfs -> handleBasicFor (cb, partsToHandle, bfs);
	    case EnhancedForStatement efs -> handleEnhancedFor (cb, partsToHandle, efs);
	    case SynchronizedStatement ss -> handleSynchronized (cb, ss);
	    case ClassInstanceCreationExpression cic -> handleNew (cb, cic);
	    case ArrayCreationExpression ace -> handleArrayCreation (cb, ace);
	    case ArrayAccess aa -> handleArrayAccess (cb, aa);
	    case StringLiteral l -> cb.ldc (l.getValue ());
	    case IntLiteral i -> handleInt (cb, i);
	    case LongLiteral l -> handleLong (cb, l);
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
	    VariableInfo vi = fa.variableInfo ();
	    if (from != null) {
		handleFrom (cb, from, vi);
	    } else {
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

	private void handleFrom (CodeBuilder cb, ParseTreeNode from, VariableInfo vi) {
	    ClassDesc owner = ClassDesc.of (FullNameHelper.type (from).getFullDollarName ());
	    String name = vi.name ();
	    ClassDesc type = ClassDesc.of (vi.typeName ().getFullDollarName ());
	    if (from instanceof ClassType ct) {
		cb.getstatic (owner, name, type);
	    } else {
		handleStatements (cb, from);
		cb.getfield (owner, name, type);
	    }
	}

	private void loadParameter (CodeBuilder cb, FormalParameterBase fpb) {
	    int slot = cb.parameterSlot (fpb.slot ());
	    FullNameHandler type = fpb.typeName ();
	    if (type == FullNameHandler.INT || type == FullNameHandler.BOOLEAN)
		cb.iload (slot);
	    else if (type == FullNameHandler.LONG)
		cb.lload (slot);
	    else if (type == FullNameHandler.DOUBLE)
		cb.dload (slot);
	    else if (type == FullNameHandler.FLOAT)
		cb.fload (slot);
	    else {  // TODO: more types
		cb.aload (slot);
	    }
	}

	private void methodInvocation (CodeBuilder cb, MethodInvocation mi) {
	    cb.lineNumber (mi.position ().getLineNumber ());

	    ParseTreeNode on = mi.getOn ();
	    if (on instanceof AmbiguousName an)
		on = an.replaced ();
	    if (!mi.isCallToStaticMethod ()) {
		if (on == null )
		    on = new ThisPrimary (mi, name);
		handleStatements (cb, on);
	    }
	    List<ParseTreeNode> args = mi.getArguments ();
	    MethodInfo info = mi.info ();
	    for (int i = 0; i < args.size (); i++) {
		ParseTreeNode a = args.get (i);
		FullNameHandler fa = FullNameHelper.type (a);
		int j = i < info.numberOfArguments () ? i : info.numberOfArguments ();
		FullNameHandler fp = info.parameter (j);
		TypeKind tkm = FullNameHelper.getTypeKind (fp);
		loadValue (cb, a, fa, fp, tkm);
	    }

	    ClassDesc owner = info.ownerDesc ();
	    String name = info.name ();
	    MethodTypeDesc type = info.methodTypeDesc ();

	    if (mi.isCallToStaticMethod ()) {
		cb.invokestatic (owner, name, type);
	    } else {
		if (isInterface (on))
		    cb.invokeinterface (owner, name, type);
		else
		    cb.invokevirtual (owner, name, type);
	    }
	}

	private boolean isInterface (ParseTreeNode on) {
	    FullNameHandler fn = FullNameHelper.type (on);
	    return cip.isInterface (fn.getFullDotName ());
	}

	private void handleReturn (CodeBuilder cb, ReturnStatement r) {
	    cb.lineNumber (r.position ().getLineNumber ());
	    FullNameHandler fm = r.type ();
	    TypeKind tkm = FullNameHelper.getTypeKind (fm);

	    ParseTreeNode p = r.expression ();
	    FullNameHandler fr = FullNameHandler.VOID;
	    if (p != null) {
		fr = FullNameHelper.type (p);
		loadValue (cb, p, fr, fm, tkm);
	    }
	    cb.returnInstruction (tkm);
	}

	private void loadValue (CodeBuilder cb, ParseTreeNode p, FullNameHandler fromType, FullNameHandler toType, TypeKind tkTo) {
	    if (fromType.isPrimitive () && toType.isPrimitive () && !fromType.equals (toType) && p instanceof NumericLiteral nl) {
		switch (tkTo) {
		case DoubleType -> cb.ldc (nl.doubleValue ());
		case FloatType -> cb.ldc (nl.floatValue ());
		case IntType -> cb.ldc (nl.intValue ());
		case LongType -> cb.ldc (nl.longValue ());
		default -> throw new IllegalStateException ("Unhandled type: " + tkTo);
		}
	    } else {
		if (p instanceof LambdaExpression le) {
		    callLambda (cb, le);
		} else {
		    handleStatements (cb, p);
		}
		widenOrAutoBoxAsNeeded (cb, fromType, toType, tkTo);
	    }
	}

	private void callLambda (CodeBuilder cb, LambdaExpression le) {
	    DirectMethodHandleDesc.Kind kind = DirectMethodHandleDesc.Kind.STATIC;
	    ClassDesc owner = ClassDesc.ofInternalName ("java/lang/invoke/LambdaMetafactory");
	    String name = "metafactory";
	    MethodTypeDesc lookupMethodType =
		MethodTypeDesc.ofDescriptor ("(" +
					     "Ljava/lang/invoke/MethodHandles$Lookup;" +  // caller
					     "Ljava/lang/String;" +                       // interface method name
					     "Ljava/lang/invoke/MethodType;" +            // factoryType
					     "Ljava/lang/invoke/MethodType;" +            // interfaceMethodType
					     "Ljava/lang/invoke/MethodHandle;" +          // implementation
					     "Ljava/lang/invoke/MethodType;" +            // dynamicMethodType
					     ")" +
					     "Ljava/lang/invoke/CallSite;");
	    DirectMethodHandleDesc bootstrapMethod =
		MethodHandleDesc.ofMethod (kind, owner, name, lookupMethodType);
	    ClassDesc ret = ClassDescUtils.getClassDesc (le.type ());
	    List<ClassDesc> types = List.of ();
	    MethodTypeDesc invocationType = MethodTypeDesc.of (ret, types);
	    MethodTypeDesc mtd = le.methodInfo ().methodTypeDesc ();
	    ClassDesc lambdaOwner = ClassDesc.of (BytecodeGenerator.this.name.getFullDollarName ());
	    String lambdaName = getLambdaName ("lambda$" + methodName + "$");
	    MethodHandleDesc mhd = MethodHandleDesc.of (DirectMethodHandleDesc.Kind.STATIC, lambdaOwner, lambdaName, mtd.descriptorString ());
	    ConstantDesc[] bootstrapArgs = {mtd, mhd, mtd}; // TODO: second mtd may require changes
	    DynamicCallSiteDesc ref = DynamicCallSiteDesc.of (bootstrapMethod, "run", invocationType, bootstrapArgs);
	    cb.invokedynamic (ref);

	    // TODO: this adds the lambda before the method we are currently building, consider queueing this up
	    addLambdaMethod (le, lambdaName, mtd);
	}

	private String getLambdaName (String prefix) {
	    AtomicInteger i = lambdaNames.computeIfAbsent (prefix, p -> new AtomicInteger ());
	    int suffix = i.getAndIncrement ();
	    return prefix + suffix;
	}

	private void addLambdaMethod (LambdaExpression le, String name, MethodTypeDesc descriptor) {
	    int flags = Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_SYNTHETIC;
	    classBuilder.withMethod (name, descriptor, flags, mb -> {
		    mb.withCode (cb -> {
			    MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, name);
			    mcb.handleStatements (cb, le.body ());
			    TypeKind returnType = FullNameHelper.getTypeKind (le.result ());
			    cb.returnInstruction (returnType);
			});
		});
	}

	private void widenOrAutoBoxAsNeeded (CodeBuilder cb, FullNameHandler from, FullNameHandler to, TypeKind tkTo) {
	    if (from.getType () == FullNameHandler.Type.PRIMITIVE) {
		if (to.getType () == FullNameHandler.Type.OBJECT) {
		    autoBox (cb, (FullNameHandler.Primitive)from);
		} else if (!to.equals (from)) {
		    TypeKind tkr = FullNameHelper.getTypeKind (from);
		    cb.convertInstruction (tkr, tkTo);
		}
	    } else if (to.getType () == FullNameHandler.Type.PRIMITIVE) {
		autoUnBox (cb, from, (FullNameHandler.Primitive)to);
	    }
	}

	// We only know we want some kind of object, we can not pass in to to get the owner
	private void autoBox (CodeBuilder cb, FullNameHandler.Primitive p) {
	    FullNameHandler ab = FullNameHelper.getAutoBoxOption (p);
	    ClassDesc owner = ClassDescUtils.getClassDesc (ab);
	    String name = "valueOf";
	    MethodTypeDesc type = MethodTypeDesc.ofDescriptor ("(" + p.getSignature () + ")L" + ab.getSlashName () + ";");
	    cb.invokestatic (owner, name, type);
	}

	// from will be java.lang.Long and to will be long, so here we can trust them
	private void autoUnBox (CodeBuilder cb, FullNameHandler from, FullNameHandler.Primitive to) {
	    ClassDesc owner = ClassDescUtils.getClassDesc (from);
	    String name = to.getFullDotName () + "Value"; // intValue, longValue, ...
	    MethodTypeDesc type = MethodTypeDesc.ofDescriptor ("()"+ to.signature ());
	    cb.invokevirtual (owner, name, type);
	}

	private void addPrimitiveCast (CodeBuilder cb, TypeKind from, TypeKind to) {
	    cb.convertInstruction (from, to);
	}

	private void handleUnaryExpression (CodeBuilder cb, UnaryExpression u) {
	    Token t = u.operator ();
	    if (t == javaTokens.MINUS) {
		ParseTreeNode exp = u.expression ();
		if (exp instanceof IntLiteral il) {
		    int value = il.getValue ();
		    handleInt (cb, -value);
		}
	    } else if (t == javaTokens.TILDE) {
		handleStatements (cb, u.expression ());
		mathOp (cb, FullNameHelper.type (u), t);
	    } else if (t == javaTokens.NOT) {
		handleStatements (cb, u.expression ());
		cb.ifThenElse (Opcode.IFEQ, b -> b.iconst_1 (), b -> b.iconst_0 ());
	    } else {
		throw new IllegalStateException ("Unhandled unary expression: " + u);
	    }
	}

	private void handleTwoPartExpression (CodeBuilder cb, TwoPartExpression two) {
	    if (isString (two)) {
		handleStringConcat (cb, two);
	    } else if (isArithmeticOrLogical (two)) {
		handleTwoPartSetup (cb, two);
		mathOp (cb, two.fullName (), two.token ());
	    } else if (two.token () == javaTokens.LOGICAL_AND) {
		handleLogicalAnd (cb, two);
	    } else if (two.token () == javaTokens.LOGICAL_OR) {
		handleLogicalOr (cb, two);
	    } else {
		// TODO: investigate if we need to evaluate to more than boolean
		FullNameHandler fnt = two.fullName ();
		ParseTreeNode p1 = two.part1 ();
		handleStatements (cb, p1);
		widen (cb, fnt, p1);

		ParseTreeNode p2 = two.part2 ();
		Opcode jumpInstruction = Opcode.IFEQ;
		if (!(fnt == FullNameHandler.BOOLEAN && p2 instanceof IntLiteral il && il.intValue () == 0)) {
		    handleStatements (cb, p2);
		    widen (cb, fnt, p2);
		    jumpInstruction = getTwoPartJump (two);
		}

		cb.ifThenElse (jumpInstruction, b -> b.iconst_1 (), b -> b.iconst_0 ());
	    }
	}

	private void handleStringConcat (CodeBuilder cb, TwoPartExpression two) {
	    List<ParseTreeNode> parts = getAllStringParts (two);
	    List<ClassDesc> types = new ArrayList<> ();
	    StringBuilder recipeBuilder = new StringBuilder ();
	    for (ParseTreeNode p : parts) {
		if (isLiteral (p)) {
		    recipeBuilder.append (p.getValue ());
		} else {
		    handleStatements (cb, p);
		    types.add (ClassDescUtils.getClassDesc (FullNameHelper.type (p)));
		    recipeBuilder.append ("\1");
		}
	    }
	    String recipe = recipeBuilder.toString ();

	    DirectMethodHandleDesc.Kind kind = DirectMethodHandleDesc.Kind.STATIC;
	    ClassDesc owner = ClassDesc.ofInternalName ("java/lang/invoke/StringConcatFactory");
	    String name = "makeConcatWithConstants";
	    MethodTypeDesc lookupMethodType =
		MethodTypeDesc.ofDescriptor ("(" +
					     "Ljava/lang/invoke/MethodHandles$Lookup;" +
					     "Ljava/lang/String;" +                        // name
					     "Ljava/lang/invoke/MethodType;" +             // concat type
					     "Ljava/lang/String;" +                        // recipe
					     "[Ljava/lang/Object;" +                       // constants
					     ")" +
					     "Ljava/lang/invoke/CallSite;");
	    DirectMethodHandleDesc bootstrapMethod =
		MethodHandleDesc.ofMethod (kind, owner, name, lookupMethodType);
	    ClassDesc ret = ClassDescUtils.getClassDesc (FullNameHandler.JL_STRING);
	    MethodTypeDesc invocationType = MethodTypeDesc.of (ret, types);
	    ConstantDesc[] bootstrapArgs = {recipe};
	    DynamicCallSiteDesc ref = DynamicCallSiteDesc.of (bootstrapMethod, name, invocationType, bootstrapArgs);
	    cb.invokedynamic (ref);
	}

	private List<ParseTreeNode> getAllStringParts (TwoPartExpression tp) {
	    List<ParseTreeNode> res = new ArrayList<> ();
	    Deque<ParseTreeNode> queue = new ArrayDeque<> ();
	    queue.addLast (tp.part1 ());
	    queue.addLast (tp.part2 ());
	    while (!queue.isEmpty ()) {
		ParseTreeNode p = queue.removeFirst ();
		if (p instanceof TwoPartExpression t && isString (t)) {
		    queue.addFirst (t.part2 ());
		    queue.addFirst (t.part1 ());
		} else {
		    res.add (p);
		}
	    }
	    return res;
	}

	private boolean isLiteral (ParseTreeNode p) {
	    // TODO: add a few more types?
	    return p instanceof NumericLiteral ||
		p instanceof StringLiteral;
	}

	private boolean isString (TwoPartExpression two) {
	    return two.fullName () == FullNameHandler.JL_STRING;
	}

	private boolean isArithmeticOrLogical (TwoPartExpression two) {
	    return javaTokens.isArithmeticOrLogical (two.token ());
	}

	private void handleTwoPartSetup (CodeBuilder cb, TwoPartExpression two) {
	    FullNameHandler fnt = two.fullName ();
	    ParseTreeNode p1 = two.part1 ();
	    handleStatements (cb, p1);
	    widen (cb, fnt, p1);
	    ParseTreeNode p2 = two.part2 ();
	    handleStatements (cb, p2);
	    widen (cb, fnt, p2);
	}

	private void widen (CodeBuilder cb, FullNameHandler targetType, ParseTreeNode p) {
	    if (targetType == FullNameHandler.BOOLEAN)
		targetType = FullNameHandler.INT;
	    FullNameHandler pfn = FullNameHelper.type (p);
	    if (pfn == targetType)
		return;
	    if (pfn.isPrimitive ()) {
		TypeKind tkm = FullNameHelper.getTypeKind (targetType);
		TypeKind tkr = FullNameHelper.getTypeKind (FullNameHelper.type (p));
		addPrimitiveCast (cb, tkr, tkm);
	    }
	}

	private void mathOp (CodeBuilder cb, FullNameHandler fullName, Token token) {
	    Map<Token, Consumer<CodeBuilder>> m = mathOps.get (fullName);
	    if (m == null)
		throw new IllegalArgumentException ("Unhandled type for math operations: " + fullName.getFullDotName ());
	    Consumer<CodeBuilder> c = m.get (token);
	    if (c == null)
		throw new IllegalArgumentException ("Unhandled type for math operations: " + fullName.getFullDotName () + ", token: " + token);
	    c.accept (cb);
	}

	private void handleLogicalAnd (CodeBuilder cb, TwoPartExpression tp) {
	    Label falseLabel = cb.newLabel ();
	    Label returnLabel = cb.newLabel ();
	    handleStatements (cb, tp.part1 ());
	    cb.ifeq (falseLabel);
	    handleStatements (cb, tp.part2 ());
	    cb.ifeq (falseLabel);
	    cb.iconst_1 ();
	    cb.goto_ (returnLabel);
	    cb.labelBinding (falseLabel);
	    cb.iconst_0 ();
	    cb.labelBinding (returnLabel);
	    cb.ireturn ();
	}

	private void handleLogicalOr (CodeBuilder cb, TwoPartExpression tp) {
	    Label trueLabel = cb.newLabel ();
	    Label falseLabel = cb.newLabel ();
	    Label returnLabel = cb.newLabel ();
	    handleStatements (cb, tp.part1 ());
	    cb.ifne (trueLabel);
	    handleStatements (cb, tp.part2 ());
	    cb.ifeq (falseLabel);
	    cb.labelBinding (trueLabel);
	    cb.iconst_1 ();
	    cb.goto_ (returnLabel);
	    cb.labelBinding (falseLabel);
	    cb.iconst_0 ();
	    cb.labelBinding (returnLabel);
	    cb.ireturn ();
	}

	private void handleTernary (CodeBuilder cb, Deque<Object> partsToHandle, Ternary t) {
	    Handler h = c -> c.ifThenElse (x -> handleStatements (x, t.thenPart ()), x -> handleStatements (x, t.elsePart ()));
	    runParts (partsToHandle, t.test (), h);
	}

	private void handleIf (CodeBuilder cb, Deque<Object> partsToHandle, IfThenStatement i) {
	    ParseTreeNode p = i.test ();
	    Opcode jumpInstruction = null;
	    if (p instanceof TwoPartExpression tp) {
		handleTwoPartSetup (cb, tp);
		jumpInstruction = getTwoPartJump (tp);
	    } else {
		handleStatements (cb, p);
		jumpInstruction = Opcode.IFNE;
	    }
	    if (i.hasElse ()) {
		cb.ifThenElse (jumpInstruction,
			       x -> handleStatements (x, i.thenPart ()),
			       x -> handleStatements (x, i.elsePart ()));
	    } else {
		cb.ifThen (jumpInstruction,
			   x -> handleStatements (x, i.thenPart ()));
	    }
	}

	private void handleAssignment (CodeBuilder cb, Deque<Object> partsToHandle, Assignment a) {
	    ParseTreeNode p = a.lhs ();
	    if (p instanceof DottedName dn)
		p = dn.replaced ();
	    ParseTreeNode value = assignmentValue (a);
	    if (p instanceof FieldAccess fa) {
		// from, value, putField
		// TODO: need to handle from better.
		ParseTreeNode from = fa.from ();
		VariableInfo vi = fa.variableInfo ();
		if (from != null) {
		    handleStatements (cb, from);
		    putField (cb, vi, value);
		} else { // this or local or static field
		    TypeKind kind = FullNameHelper.getTypeKind (vi.typeName ());
		    switch (vi.fieldType ()) {
		    case VariableInfo.Type.FIELD ->
			putField (cb, vi, value);
		    case VariableInfo.Type.PARAMETER ->
			putInLocalSlot (cb, kind, ((FormalParameterBase)vi).slot (), value);
		    case VariableInfo.Type.LOCAL ->
			putInLocalSlot (cb, kind, ((LocalVariable)vi).slot (), value);
		    }
		}
	    } else if (p instanceof ArrayAccess aa) {
		// field, slot, value, arraystore
		TypeKind kind = FullNameHelper.getTypeKind (FullNameHelper.type (p));
		handleStatements (cb, List.of (aa.from (), aa.slot (), value));
		cb.arrayStoreInstruction (kind);
	    } else {
		throw new IllegalStateException ("Unhandled assignment type: " + p + ", " + p.getClass ().getName () +
						 ", " + p.position ().toShortString ());
	    }
	}

	private ParseTreeNode assignmentValue (Assignment a) {
	    Token op = a.operator ();
	    if (op == javaTokens.EQUAL)
		return a.rhs ();
	    String name = op.getName ();
	    // not sure about !=, <= >= but they are not assignments.
	    if (name.endsWith ("=")) {
		String newOp = name.substring (0, name.length () - 1);
		Token t = grammar.getExistingToken (newOp);
		// TODO: we might need to add cast to right type
		TwoPartExpression tp = new TwoPartExpression (a.lhs (), t, a.rhs ());
		tp.fullName (a.fullName ());
		return tp;
	    }

	    throw new IllegalStateException ("Unahandled operator: " + a);
	}

	private void putInLocalSlot (CodeBuilder cb, TypeKind kind, int slot, ParseTreeNode value) {
	    handleStatements (cb, value);
	    cb.storeInstruction (kind, slot);
	}

	private void handleLocalVariables (CodeBuilder cb, LocalVariableDeclaration lvs) {
	    FullNameHandler fn = FullNameHelper.type (lvs.getType ());
	    for (VariableDeclarator lv : lvs.getDeclarators ()) {
		TypeKind kind = FullNameHelper.getTypeKind (fn);
		int slot = cb.allocateLocal (kind);
		lv.localSlot (slot);
		if (lv.hasInitializer ()) {
		    handleStatements (cb, lv.initializer ());
		    FullNameHandler fromType = FullNameHelper.type (lv.initializer ());
		    widenOrAutoBoxAsNeeded (cb, fromType, fn, kind);
		    cb.storeInstruction (kind, slot);
		}
	    }
	}

	private void handlePostIncrement (CodeBuilder cb, Deque<Object> partsToHandle, PostIncrementExpression pie) {
	    handlePostChange (cb, partsToHandle, pie.expression (), 1);
	}

	private void handlePostDecrement (CodeBuilder cb, Deque<Object> partsToHandle, PostDecrementExpression pde) {
	    handlePostChange (cb, partsToHandle, pde.expression (), -1);
	}

	private void handlePostChange (CodeBuilder cb, Deque<Object> partsToHandle, ParseTreeNode tn, int change) {
	    if (tn instanceof DottedName dn)
		tn = dn.replaced ();
	    if (tn instanceof FieldAccess fa) {
		ParseTreeNode from = fa.from ();
		if (from != null) {
		    // TODO: implement
		} else {
		    VariableInfo vi = fa.variableInfo ();
		    switch (vi.fieldType ()) {
		    case VariableInfo.Type.PARAMETER -> incrementLocalVariable (cb, ((FormalParameterBase)vi).slot (), change);
		    case VariableInfo.Type.LOCAL -> incrementLocalVariable (cb, ((LocalVariable)vi).slot (), change);
		    case VariableInfo.Type.FIELD -> incrementField (cb, vi, change);
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

	private void incrementField (CodeBuilder cb, VariableInfo vi, int change) {
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
	    handleInt (cb, change);
	    cb.iadd ();
	    if (isInstanceField (vi))
		cb.putfield (owner, vi.name (), type);
	    else
		cb.putstatic (owner, vi.name (), type);
	}

	private void handleBasicFor (CodeBuilder cb, Deque<Object> partsToHandle, BasicForStatement bfs) {
	    ParseTreeNode forInit = bfs.forInit ();
	    ParseTreeNode expression = bfs.expression ();
	    ParseTreeNode forUpdate = bfs.forUpdate ();
	    ParseTreeNode statement = bfs.statement ();

	    if (forInit != null)
		handleStatements (cb, forInit);
	    Label lExp = cb.newBoundLabel ();
	    Label lEnd = cb.newLabel ();
	    if (expression != null) {
		handleForExpression (cb, expression, lEnd);
	    }
	    handleStatements (cb, statement);
	    handleStatements (cb, forUpdate);
	    cb.goto_ (lExp); // what about goto_w?
	    cb.labelBinding (lEnd);
	}

	private void handleForExpression (CodeBuilder cb, ParseTreeNode exp, Label endLabel) {
	    Opcode operator = Opcode.IFEQ;
	    if (exp instanceof UnaryExpression) {
		handleStatements (cb, exp);
		operator = Opcode.IFNE;
	    } else if (exp instanceof TwoPartExpression tp) {
		handleStatements (cb, tp.part1 ());

		ParseTreeNode p2 = tp.part2 ();
		if (p2 instanceof IntLiteral il && il.intValue () == 0) {
		    operator = getReverseZeroJump (tp.token ());
		} else {
		    handleStatements (cb, tp.part2 ());
		    operator = getReverseTwoPartJump (tp);
		}
	    }
	    cb.branchInstruction (operator, endLabel);
	}

	private void handleEnhancedFor (CodeBuilder cb, Deque<Object> partsToHandle, EnhancedForStatement efs) {
	    ParseTreeNode exp = efs.expression ();
	    FullNameHandler fn = FullNameHelper.type (exp);
	    if (fn.isArray ()) {
		handleArrayLoop (cb, efs, fn);
	    } else {
		handleIteratorLoop (cb, efs, fn);
	    }
	}

	/*
	  static int a (int[] x) {          Transformed into this:
	  int r = 0;                    int r = 0;
	  int[] xc = x; int s = xc.length;
	  for (int a : x)               for (int i = 0; i < s; i++) {
	  r += a;                        a = xc[i]; r += a;
	  }                             }
	  return r;                     return r;
	  }}
	*/
	private void handleArrayLoop (CodeBuilder cb, EnhancedForStatement efs, FullNameHandler fn) {
	    LocalVariableDeclaration lv = efs.localVariable ();
	    List<VariableDeclarator> vds = lv.getDeclarators ();
	    if (vds.size () > 1)
		throw new IllegalStateException ("Unable to handle more than one variable in EnhancedForStatement.");
	    VariableDeclarator vd = vds.get (0);
	    FullNameHandler varName = FullNameHelper.type (lv);
	    TypeKind varKind = FullNameHelper.getTypeKind (varName);

	    TypeKind kind = FullNameHelper.getTypeKind (fn);
	    ArrayInfo ai = ArrayInfo.create (cb, kind);
	    arrayLoopSetup (cb, efs, ai);                    // copy array, store length

	    Label loopLabel = cb.newBoundLabel ();
	    Label endLabel = cb.newLabel ();
	    arrayLoopIndexCheck (cb, ai, endLabel);          // i < s
	    storeArrayLoopValue (cb, ai, lv, vd, varKind);   // a = xc[i]
	    handleStatements (cb, efs.statement ());
	    cb.iinc (ai.indexSlot (), 1);                    // i++
	    cb.goto_ (loopLabel); // what about goto_w?
	    cb.labelBinding (endLabel);
	}

	private void arrayLoopSetup (CodeBuilder cb, EnhancedForStatement efs, ArrayInfo ai) {
	    handleStatements (cb, efs.expression ());
	    cb.astore (ai.arrayCopySlot ());
	    cb.aload (ai.arrayCopySlot ());
	    cb.arraylength ();
	    cb.istore (ai.lengthSlot ());
	    cb.iconst_0 ();
	    cb.istore (ai.indexSlot ());
	}

	private void arrayLoopIndexCheck (CodeBuilder cb, ArrayInfo ai, Label endLabel) {
	    cb.iload (ai.indexSlot ());
	    cb.iload (ai.lengthSlot ());
	    cb.if_icmpeq (endLabel);
	}

	private void storeArrayLoopValue (CodeBuilder cb, ArrayInfo ai, LocalVariableDeclaration lv,
					  VariableDeclarator vd, TypeKind varKind) {
	    cb.aload (ai.arrayCopySlot ());
	    cb.iload (ai.indexSlot ());
	    cb.iaload ();
	    handleLocalVariables (cb, lv);
	    int varSlot = vd.slot ();
	    cb.storeInstruction (varKind, varSlot);
	}

	private record ArrayInfo (int arrayCopySlot, int lengthSlot, int indexSlot) {
	    public static ArrayInfo create (CodeBuilder cb, TypeKind kind) {
		return new ArrayInfo (cb.allocateLocal (kind), cb.allocateLocal (TypeKind.IntType), cb.allocateLocal (TypeKind.IntType));
	    }
	}

	private void handleIteratorLoop (CodeBuilder cb, EnhancedForStatement efs, FullNameHandler fn) {
	    int iteratorSlot = cb.allocateLocal (TypeKind.ReferenceType);
	    handleStatements (cb, efs.expression ());
	    ClassDesc owner = ClassDescUtils.getClassDesc (fn);
	    ClassDesc iteratorDesc = ClassDesc.of ("java.util.Iterator");
	    MethodTypeDesc type = MethodTypeDesc.of (iteratorDesc);
	    cb.invokeinterface (owner, "iterator", type);
	    cb.astore (iteratorSlot);

	    Label loopLabel = cb.newBoundLabel ();
	    Label endLabel = cb.newLabel ();

	    cb.aload (iteratorSlot);
	    type = MethodTypeDesc.ofDescriptor ("()Z");
	    cb.invokeinterface (iteratorDesc, "hasNext", type);
	    cb.ifeq (endLabel);

	    cb.aload (iteratorSlot);
	    type = MethodTypeDesc.of (ConstantDescs.CD_Object);
	    cb.invokeinterface (iteratorDesc, "next", type);
	    // TODO: add: cb.checkcast (genericType);

	    LocalVariableDeclaration lv = efs.localVariable ();
	    handleLocalVariables (cb, lv);
	    int varSlot = lv.getDeclarators ().get (0).slot ();
	    cb.storeInstruction (TypeKind.ReferenceType, varSlot);

	    handleStatements (cb, efs.statement ());

	    cb.goto_ (loopLabel); // what about goto_w?
	    cb.labelBinding (endLabel);
	}

	private Opcode getReverseZeroJump (Token t) {
	    if (t == javaTokens.DOUBLE_EQUAL) return Opcode.IFNE;
	    if (t == javaTokens.NOT_EQUAL) return Opcode.IFEQ;
	    if (t == javaTokens.LT) return Opcode.IFGE;
	    if (t == javaTokens.GT) return Opcode.IFLE;
	    if (t == javaTokens.LE) return Opcode.IFGT;
	    if (t == javaTokens.GE) return Opcode.IFLT;
	    throw new IllegalArgumentException ("Unknown zero comparisson: " + t);
	}

	private Opcode getTwoPartJump (TwoPartExpression t) {
	    return getForwardJump (t.token (), t.optype () == TwoPartExpression.OpType.PRIMITIVE_OP);
	}

	private Opcode getReverseTwoPartJump (TwoPartExpression t) {
	    return getReversedJump (t.token (), t.optype () == TwoPartExpression.OpType.PRIMITIVE_OP);
	}

	private Opcode getForwardJump (Token token, boolean primitiveExpression) {
	    if (primitiveExpression) {
		if (token == javaTokens.DOUBLE_EQUAL) return Opcode.IF_ICMPEQ;
		if (token == javaTokens.NOT_EQUAL) return Opcode.IF_ICMPNE;
		if (token == javaTokens.LT) return Opcode.IF_ICMPLT;
		if (token == javaTokens.GT) return Opcode.IF_ICMPGT;
		if (token == javaTokens.LE) return Opcode.IF_ICMPLE;
		if (token == javaTokens.GE) return Opcode.IF_ICMPGE;
		throw new IllegalStateException ("unhandled primitive jump type: " + token);
	    }
	    if (token == javaTokens.DOUBLE_EQUAL)
		return Opcode.IF_ACMPEQ;
	    else if (token == javaTokens.NOT_EQUAL)
		return Opcode.IF_ACMPNE;
	    else
		throw new IllegalStateException ("unhandled jump type: " + token);
	}

	private Opcode getReversedJump (Token token, boolean primitiveExpression) {
	    if (primitiveExpression) {
		if (token == javaTokens.DOUBLE_EQUAL) return Opcode.IF_ICMPNE;
		if (token == javaTokens.NOT_EQUAL) return Opcode.IF_ICMPEQ;
		if (token == javaTokens.LT) return Opcode.IF_ICMPGE;
		if (token == javaTokens.GT) return Opcode.IF_ICMPLE;
		if (token == javaTokens.LE) return Opcode.IF_ICMPGT;
		if (token == javaTokens.GE) return Opcode.IF_ICMPLT;
		throw new IllegalStateException ("unhandled primitive jump type: " + token);
	    }
	    if (token == javaTokens.DOUBLE_EQUAL)
		return Opcode.IF_ACMPNE;
	    else if (token == javaTokens.NOT_EQUAL)
		return Opcode.IF_ACMPEQ;
	    else
		throw new IllegalStateException ("unhandled jump type: " + token);
	}

	private void handleSynchronized (CodeBuilder cb, SynchronizedStatement ss) {
	    handleStatements (cb, ss.expression ());
	    cb.dup ();
	    int expressionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	    cb.astore (expressionSlot);
	    cb.monitorenter ();

	    Label endLabel = cb.newLabel ();
	    Label monitorStart = cb.newBoundLabel ();
	    handleStatements (cb, ss.block ());

	    cb.aload (expressionSlot);
	    cb.monitorexit ();
	    Label monitorEnd = cb.newBoundLabel ();
	    cb.goto_ (endLabel);

	    Label handlerStart = cb.newBoundLabel ();
	    cb.exceptionCatch (monitorStart, monitorEnd, handlerStart, Optional.empty ());
	    int exceptionSlot = cb.allocateLocal (TypeKind.ReferenceType);
	    cb.astore (exceptionSlot);
	    cb.aload (expressionSlot);
	    cb.monitorexit ();
	    Label handlerEnd = cb.newBoundLabel ();
	    cb.aload (exceptionSlot);
	    cb.athrow ();
	    cb.exceptionCatch (handlerStart, handlerEnd, handlerStart, Optional.empty ());

	    cb.labelBinding (endLabel);
	}

	private void handleNew (CodeBuilder cb, ClassInstanceCreationExpression cic) {
	    ClassDesc cd = ClassDescUtils.getClassDesc (cic.type ());
	    cb.new_ (cd);
	    cb.dup ();
	    cb.invokespecial (cd, INSTANCE_INIT, INIT_SIGNATURE);
	}

	private void handleArrayCreation (CodeBuilder cb, ArrayCreationExpression ace) {
	    handleStatements (cb, ace.getChildren ());
	    FullNameHandler type = ace.innerFullName ();
	    if (ace.rank () == 1) {
		if (type.isPrimitive ()) {
		    TypeKind kind = FullNameHelper.getTypeKind (type);
		    cb.newarray (kind);
		} else {
		    ClassDesc desc = ClassDescUtils.getClassDesc (type);
		    cb.anewarray (desc);
		}
	    } else {
		ClassDesc desc = ClassDescUtils.getClassDesc (ace.fullName ());
		cb.multianewarray (desc, ace.dimExprs ());
	    }
	}

	private void handleArrayAccess (CodeBuilder cb, ArrayAccess aa) {
	    handleStatements (cb, aa.from ());
	    handleStatements (cb, aa.slot ());
	    TypeKind tk = FullNameHelper.getTypeKind (FullNameHelper.type (aa));
	    cb.arrayLoadInstruction (tk);
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

	private void handleLong (CodeBuilder cb, LongLiteral l) {
	    cb.ldc (l.longValue ());
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
	    else if (t == javaTokens.TRUE)
		cb.iconst_1 ();
	    else if (t == javaTokens.FALSE)
		cb.iconst_0 ();
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
