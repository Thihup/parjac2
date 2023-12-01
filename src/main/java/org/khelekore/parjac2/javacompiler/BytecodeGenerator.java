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
import io.github.dmlloyd.classfile.attribute.SignatureAttribute;
import io.github.dmlloyd.classfile.attribute.SourceFileAttribute;

import org.khelekore.parjac2.javacompiler.code.ArrayGenerator;
import org.khelekore.parjac2.javacompiler.code.AttributeHelper;
import org.khelekore.parjac2.javacompiler.code.CodeUtil;
import org.khelekore.parjac2.javacompiler.code.SwitchGenerator;
import org.khelekore.parjac2.javacompiler.code.SynchronizationGenerator;
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

    public static final String INSTANCE_INIT = "<init>";
    public static final MethodTypeDesc INIT_SIGNATURE = MethodTypeDesc.ofDescriptor ("()V");

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
	String signature = SignatureHelper.getClassSignature (cip, genericTypeHelper, c.getTypeParameters (),
							      c.getSuperClass (), c.getSuperInterfaces ());
	return generateClass (ImplicitClassFlags.CLASS_FLAGS, signature,
			      c.getSuperClass (), c.getSuperInterfaces ());
    }

    private byte[] generateClass (EnumDeclaration e) {
	String signature = "Ljava/lang/Enum<L" + name.getSlashName () + ";>;";
	return generateClass (ImplicitClassFlags.ENUM_FLAGS, signature,
			      enumClassType, List.of ());
    }

    private byte[] generateClass (RecordDeclaration r) {
	String signature = SignatureHelper.getClassSignature (cip, genericTypeHelper, r.getTypeParameters (),
							      r.getSuperClass (), r.getSuperInterfaces ());
	return generateClass (ImplicitClassFlags.RECORD_FLAGS, signature,
			      recordClassType, List.of ());
    }

    private byte[] generateInterface (NormalInterfaceDeclaration i) {
	String signature = SignatureHelper.getClassSignature (cip, genericTypeHelper, i.getTypeParameters (),
							      null, i.getExtendsInterfaces ());
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

		AttributeHelper.addInnerClassAttributes (classBuilder, cip, td);
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
		SignatureHelper.MethodSignatureHolder msh = getMethodSignature (c);
		classBuilder.withMethod (ConstantDescs.INIT_NAME, msh.desc (), flags, mb -> {
			mb.withCode (cb -> {
				MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, INSTANCE_INIT, flags);
				mcb.createConstructorContents (cb, c, msh);
			    });
			if (msh.signature () != null)
			    mb.with (SignatureAttribute.of (MethodSignature.parseFrom (msh.signature ())));
		    });
	    });
    }

    private void addMethods (ClassBuilder classBuilder, TypeDeclaration td) {
	td.getMethods ().forEach (m -> {
		SignatureHelper.MethodSignatureHolder msh = getMethodSignature (m);
		int flags = m.flags ();
		TypeKind returnType = FullNameHelper.getTypeKind (m.result ());
		ParseTreeNode body = m.getMethodBody ();
		classBuilder.withMethod (m.name (), msh.desc (), flags, mb -> {
			if (!m.isAbstract ()) {
			    mb.withCode (cb -> {
				    MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, m.name (), flags);
				    mcb.addMethodContent (cb, (Block)body, returnType);
				});
			}
			if (msh.signature () != null)
			    mb.with (SignatureAttribute.of (MethodSignature.parseFrom (msh.signature ())));
		    });
	    });
    }

    private void addStaticBlocks (ClassBuilder classBuilder, TypeDeclaration td) {
	List<SyntaxTreeNode> ls = td.getStaticInitializers ();
	if (ls.isEmpty ())
	    return;

	int flags = Flags.ACC_STATIC;
	classBuilder.withMethod (STATIC_INIT, INIT_SIGNATURE, flags, mb -> {
		for (SyntaxTreeNode si : ls)
		    mb.withCode (cb -> {
			    MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, STATIC_INIT, flags);
			    mcb.handleStatements (cb, si);
			    cb.return_ ();
			});
	    });
    }

    private class MethodContentBuilder implements MethodContentGenerator {
	private final ClassBuilder classBuilder;
	private final String methodName;
	private final int flags;

	public MethodContentBuilder (ClassBuilder classBuilder, String methodName, int flags) {
	    this.classBuilder = classBuilder;
	    this.methodName = methodName;
	    this.flags = flags;
	}

	private void createConstructorContents (CodeBuilder cb, ConstructorDeclarationInfo cdb, SignatureHelper.MethodSignatureHolder msh) {
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
		CodeUtil.callSuperInit (cb, td, cdb);

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

	@Override public void handleStatements (CodeBuilder cb, List<? extends ParseTreeNode> statements) {
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
	    case Ternary t -> handleTernary (cb, t);
	    case IfThenStatement ifts -> handleIf (cb, ifts);
	    case Assignment a -> handleAssignment (cb, partsToHandle, a);
	    case LocalVariableDeclaration lv -> handleLocalVariables (cb, lv);
	    case PostIncrementExpression pie -> handlePostIncrement (cb, pie);
	    case PostDecrementExpression pde -> handlePostDecrement (cb, pde);
	    case BasicForStatement bfs -> handleBasicFor (cb, bfs);
	    case EnhancedForStatement efs -> handleEnhancedFor (cb, efs);
	    case SynchronizedStatement ss -> SynchronizationGenerator.handleSynchronized (this, cb, ss);
	    case ClassInstanceCreationExpression cic -> CodeUtil.callNew (cb, cic);
	    case ArrayCreationExpression ace -> ArrayGenerator.handleArrayCreation (this, cb, ace);
	    case ArrayAccess aa -> ArrayGenerator.handleArrayAccess (this, cb, aa);
	    case SwitchExpression se -> SwitchGenerator.handleSwitchExpression (this, cb, se);

	    // We get LambdaExpression and MethodReference in Assignment, so we just want to store the handle to it
	    case LambdaExpression le -> callLambda (cb, le);
	    case MethodReference mr -> callMethodReference (cb, mr);

	    case StringLiteral l -> cb.ldc (l.getValue ());
	    case IntLiteral i -> CodeUtil.handleInt (cb, i);
	    case LongLiteral l -> CodeUtil.handleLong (cb, l);
	    case DoubleLiteral d -> CodeUtil.handleDouble (cb, d);
	    case ThisPrimary t -> CodeUtil.handleThis (cb);
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
		    CodeUtil.loadParameter (cb, (FormalParameterBase)vi);
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
		CodeUtil.widenOrAutoBoxAsNeeded (cb, fromType, toType, tkTo);
	    }
	}

	private void callLambda (CodeBuilder cb, LambdaExpression le) {
	    String lambdaName = getLambdaName ("lambda$" + methodName + "$");
	    MethodTypeDesc mtd = le.methodInfo ().methodTypeDesc ();
	    callDynamic (cb, lambdaName, mtd, le.type (), false);
	    // TODO: this adds the lambda before the method we are currently building, consider queueing this up
	    addLambdaMethod (le, lambdaName, mtd);
	}

	private void callMethodReference (CodeBuilder cb, MethodReference mr) {
	    MethodInfo info = mr.methodInfo ();
	    boolean forceStatic = Flags.isStatic (mr.actualMethod ().flags ());
	    MethodTypeDesc mtd = info.methodTypeDesc ();
	    callDynamic (cb, mr.name (), mtd, mr.type (), forceStatic);
	}

	private void callDynamic (CodeBuilder cb, String dynamicMethod, MethodTypeDesc mtd, FullNameHandler dynamicType, boolean forceStatic) {
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
	    ClassDesc lambdaOwner = ClassDesc.of (BytecodeGenerator.this.name.getFullDollarName ());

	    DirectMethodHandleDesc.Kind dmk = DirectMethodHandleDesc.Kind.STATIC;
	    List<ClassDesc> types = List.of ();
	    if (!forceStatic && !Flags.isStatic (flags)) {
		dmk = DirectMethodHandleDesc.Kind.VIRTUAL;
		cb.aload (0);
		types = List.of (ClassDescUtils.getClassDesc (BytecodeGenerator.this.name));
	    }
	    ClassDesc ret = ClassDescUtils.getClassDesc (dynamicType);
	    MethodTypeDesc invocationType = MethodTypeDesc.of (ret, types);
	    MethodHandleDesc mhd = MethodHandleDesc.of (dmk, lambdaOwner, dynamicMethod, mtd.descriptorString ());
	    ConstantDesc[] bootstrapArgs = {mtd, mhd, mtd}; // TODO: second mtd may require changes
	    DynamicCallSiteDesc ref = DynamicCallSiteDesc.of (bootstrapMethod, "run", invocationType, bootstrapArgs);
	    cb.invokedynamic (ref);
	}

	private String getLambdaName (String prefix) {
	    AtomicInteger i = lambdaNames.computeIfAbsent (prefix, p -> new AtomicInteger ());
	    int suffix = i.getAndIncrement ();
	    return prefix + suffix;
	}

	private void addLambdaMethod (LambdaExpression le, String name, MethodTypeDesc descriptor) {
	    int lf = Flags.ACC_PRIVATE | Flags.ACC_SYNTHETIC;
	    if (Flags.isStatic (flags))
		lf |= Flags.ACC_STATIC;
	    int lambdaFlags = lf;
	    classBuilder.withMethod (name, descriptor, lambdaFlags, mb -> {
		    mb.withCode (cb -> {
			    MethodContentBuilder mcb = new MethodContentBuilder (classBuilder, name, lambdaFlags);
			    mcb.handleStatements (cb, le.body ());
			    TypeKind returnType = FullNameHelper.getTypeKind (le.result ());
			    cb.returnInstruction (returnType);
			});
		});
	}

	private void handleUnaryExpression (CodeBuilder cb, UnaryExpression u) {
	    Token t = u.operator ();
	    if (t == javaTokens.MINUS) {
		ParseTreeNode exp = u.expression ();
		if (exp instanceof IntLiteral il) {
		    int value = il.getValue ();
		    CodeUtil.handleInt (cb, -value);
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

	// for statements outside of if-tests and similar
	private void handleTwoPartExpression (CodeBuilder cb, TwoPartExpression two) {
	    if (CodeUtil.isString (two)) {
		handleStringConcat (cb, two);
	    } else if (isArithmeticOrLogical (two)) {
		handleTwoPartSetup (cb, two);
		mathOp (cb, two.fullName (), two.token ());
	    } else if (two.token () == javaTokens.LOGICAL_AND) {
		handleLogicalAnd (cb, two);
	    } else if (two.token () == javaTokens.LOGICAL_OR) {
		handleLogicalOr (cb, two);
	    } else if (two.token () == javaTokens.INSTANCEOF) {
		Label elseLabel = cb.newLabel ();
		handleInstanceOf (cb, two, elseLabel);
		cb.labelBinding (elseLabel);
	    } else {
		Opcode jumpInstruction = handleOtherTwoPart (cb, two);
		cb.ifThenElse (jumpInstruction, b -> b.iconst_1 (), b -> b.iconst_0 ());
	    }
	}

	private void handleStringConcat (CodeBuilder cb, TwoPartExpression two) {
	    List<ParseTreeNode> parts = getAllStringParts (two);
	    List<ClassDesc> types = new ArrayList<> ();
	    StringBuilder recipeBuilder = new StringBuilder ();
	    for (ParseTreeNode p : parts) {
		if (CodeUtil.isLiteral (p)) {
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
		if (p instanceof TwoPartExpression t && CodeUtil.isString (t)) {
		    queue.addFirst (t.part2 ());
		    queue.addFirst (t.part1 ());
		} else {
		    res.add (p);
		}
	    }
	    return res;
	}

	private boolean isArithmeticOrLogical (TwoPartExpression two) {
	    return javaTokens.isArithmeticOrLogical (two.token ());
	}

	private void handleTwoPartSetup (CodeBuilder cb, TwoPartExpression two) {
	    FullNameHandler fnt = two.fullName ();
	    ParseTreeNode p1 = two.part1 ();
	    handleStatements (cb, p1);
	    CodeUtil.widen (cb, fnt, p1);
	    ParseTreeNode p2 = two.part2 ();
	    handleStatements (cb, p2);
	    CodeUtil.widen (cb, fnt, p2);
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

	private void handleInstanceOf (CodeBuilder cb, TwoPartExpression tp, Label elseLabel) {
	    handleStatements (cb, tp.part1 ());
	    ParseTreeNode p2 = tp.part2 ();
	    FullNameHandler check = FullNameHelper.type (p2);
	    cb.instanceof_ (ClassDescUtils.getClassDesc (check));
	    if (p2 instanceof LocalVariableDeclaration lvd) {
		cb.ifeq (elseLabel);
		handleLocalVariables (cb, lvd);                    // get slot for variable
		handleStatements (cb, tp.part1 ());
		cb.checkcast (ClassDescUtils.getClassDesc (check));
		cb.astore (lvd.getDeclarators ().get (0).slot ()); // only one!
	    }
	}

	private Opcode handleOtherTwoPart (CodeBuilder cb, TwoPartExpression two) {
	    FullNameHandler fnt = two.fullName ();
	    ParseTreeNode p1 = two.part1 ();
	    handleStatements (cb, p1);
	    CodeUtil.widen (cb, fnt, p1);

	    ParseTreeNode p2 = two.part2 ();
	    Opcode jumpInstruction = Opcode.IFEQ;
	    if (!(fnt == FullNameHandler.BOOLEAN && p2 instanceof IntLiteral il && il.intValue () == 0)) {
		handleStatements (cb, p2);
		CodeUtil.widen (cb, fnt, p2);
		jumpInstruction = getTwoPartJump (two);
	    }
	    return jumpInstruction;
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

	private void handleTernary (CodeBuilder cb, Ternary t) {
	    handleGenericIfElse (cb, t.test (), t.thenPart (), t.elsePart ());
	}

	private void handleIf (CodeBuilder cb, IfThenStatement i) {
	    handleGenericIfElse (cb, i.test (), i.thenPart (), i.elsePart ());
	}

	private void handleGenericIfElse (CodeBuilder cb, ParseTreeNode test, ParseTreeNode thenPart, ParseTreeNode elsePart) {
	    Opcode jumpInstruction = null;
	    if (test instanceof TwoPartExpression tp) {
		Token token = tp.token ();
		if (isShortCircut (token)) {
		    handleLogicalIf (cb, thenPart, elsePart, tp);
		    return;
		}

		if (token == javaTokens.INSTANCEOF) {
		    handleStatements (cb, tp.part1 ());
		    FullNameHandler check = FullNameHelper.type (tp.part2 ());
		    cb.instanceof_ (ClassDescUtils.getClassDesc (check));
		    jumpInstruction = Opcode.IFNE; // jump inverts, so we will use IFEQ
		} else {
		    handleTwoPartSetup (cb, tp);
		    jumpInstruction = getTwoPartJump (tp);
		}
	    } else {
		handleStatements (cb, test);
		jumpInstruction = Opcode.IFNE;
	    }
	    if (elsePart != null) {
		cb.ifThenElse (jumpInstruction,
			       x -> handleStatements (x, thenPart),
			       x -> handleStatements (x, elsePart));
	    } else {
		cb.ifThen (jumpInstruction,
			   x -> handleStatements (x, thenPart));
	    }
	}

	private void handleLogicalIf (CodeBuilder cb, ParseTreeNode thenPart, ParseTreeNode elsePart, TwoPartExpression tp) {
	    Label thenLabel = cb.newLabel ();
	    Label elseLabel = cb.newLabel ();
	    handleLogicalChain (cb, tp, thenLabel, elseLabel, false);
	    handleIfThenParts (cb, thenPart, elsePart, thenLabel, elseLabel);
	}


	/*
	  On their own:
	  && first test: false -> jump to elseLabel
	  && second test: false -> jump to elseLabel
	  || first test: true -> jump to thenLabel
	  || second test: false -> jump to elseLabel

	  Howerver
	  && inside || need change:
	  && first test: false -> jump to elseLabel
	  && second test: true -> jump to thenLabel
	*/
	private void handleLogicalChain (CodeBuilder cb, TwoPartExpression tp, Label thenLabel, Label elseLabel, boolean insideOr) {
	    ParseTreeNode p1 = tp.part1 ();
	    ParseTreeNode p2 = tp.part2 ();
	    boolean isOr = tp.token () == javaTokens.LOGICAL_OR;

	    Label nextOption = null;
	    if (isOr)
		nextOption = cb.newLabel ();
	    TwoPartExpression tp2 = null;
	    if (p1 instanceof TwoPartExpression two)
		tp2 = two;
	    if (tp2 != null && isShortCircut (tp2.token ())) {
		if (isOr)
		    handleLogicalChain (cb, tp2, thenLabel, nextOption, true);
		else
		    handleLogicalChain (cb, tp2, thenLabel, elseLabel, false);
	    } else if (tp2 != null && tp2.token () == javaTokens.INSTANCEOF) {
		handleInstanceOf (cb, tp2, elseLabel);
	    } else if (tp2 != null) {
		handleOtherTwoPart (cb, tp2);
		cb.branchInstruction (getReverseTwoPartJump (tp2), elseLabel);
	    } else {
		handleStatements (cb, p1);
		firstTest (cb, isOr, thenLabel, elseLabel);
	    }
	    if (isOr)
		cb.labelBinding (nextOption);

	    tp2 = p2 instanceof TwoPartExpression two ? two : null;
	    if (tp2 != null && isShortCircut (tp2.token ())) {
		handleLogicalChain (cb, tp2, thenLabel, elseLabel, false);
	    } else if (tp2 != null && tp2.token () == javaTokens.INSTANCEOF) {
		handleInstanceOf (cb, tp2, elseLabel);
	    } else if (tp2 != null) {
		handleOtherTwoPart (cb, tp2);
		cb.branchInstruction (getReverseTwoPartJump (tp2), elseLabel);
	    } else {
		handleStatements (cb, p2);
		secondTest (cb, isOr, thenLabel, elseLabel, insideOr);
	    }
	}

	private void firstTest (CodeBuilder cb, boolean isOr, Label thenLabel, Label elseLabel) {
	    if (isOr)
		cb.ifne (thenLabel);
	    else
		cb.ifeq (elseLabel);
	}

	private void secondTest (CodeBuilder cb, boolean isOr, Label thenLabel, Label elseLabel, boolean insideOr) {
	    if (isOr)
		cb.ifeq (elseLabel);
	    else if (insideOr)
		cb.ifne (thenLabel);
	    else
		cb.ifeq (elseLabel);
	}

	private void handleIfThenParts (CodeBuilder cb, ParseTreeNode thenPart, ParseTreeNode elsePart, Label thenLabel, Label elseLabel) {
	    cb.labelBinding (thenLabel);
	    handleStatements (cb, thenPart);
	    Label endLabel = cb.newLabel ();
	    if (!endsWithReturn (thenPart))
		cb.goto_ (endLabel);
	    cb.labelBinding (elseLabel);
	    if (elsePart != null)
		handleStatements (cb, elsePart);
	    cb.labelBinding (endLabel);
	}

	private boolean endsWithReturn (ParseTreeNode p) {
	    // TOOD: we might need more cases
	    if (p instanceof ReturnStatement)
		return true;
	    if (p instanceof Block b) {
		List<ParseTreeNode> ls = b.get ();
		if (!ls.isEmpty () && ls.getLast () instanceof ReturnStatement)
		    return true;
	    }
	    return false;
	}

	private boolean isShortCircut (Token token) {
	    return token == javaTokens.LOGICAL_AND || token == javaTokens.LOGICAL_OR;
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
		    CodeUtil.widenOrAutoBoxAsNeeded (cb, fromType, fn, kind);
		    cb.storeInstruction (kind, slot);
		}
	    }
	}

	private void handlePostIncrement (CodeBuilder cb, PostIncrementExpression pie) {
	    handlePostChange (cb, pie.expression (), 1);
	}

	private void handlePostDecrement (CodeBuilder cb, PostDecrementExpression pde) {
	    handlePostChange (cb, pde.expression (), -1);
	}

	private void handlePostChange (CodeBuilder cb, ParseTreeNode tn, int change) {
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
	    CodeUtil.handleInt (cb, change);
	    cb.iadd ();
	    if (isInstanceField (vi))
		cb.putfield (owner, vi.name (), type);
	    else
		cb.putstatic (owner, vi.name (), type);
	}

	private void handleBasicFor (CodeBuilder cb, BasicForStatement bfs) {
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

	private void handleEnhancedFor (CodeBuilder cb, EnhancedForStatement efs) {
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

    private SignatureHelper.MethodSignatureHolder getMethodSignature (ConstructorDeclarationInfo c) {
	return SignatureHelper.getMethodSignature (cip, genericTypeHelper, c.getTypeParameters (), c.getFormalParameterList (), VOID_RETURN);
    }

    private SignatureHelper.MethodSignatureHolder getMethodSignature (MethodDeclarationBase m) {
	return SignatureHelper.getMethodSignature (cip, genericTypeHelper, m.getTypeParameters (), m.getFormalParameterList (), m.getResult ());
    }
}
