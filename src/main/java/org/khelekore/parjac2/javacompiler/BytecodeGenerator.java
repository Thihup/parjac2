package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
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
import io.github.dmlloyd.classfile.ClassFile;
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
import org.khelekore.parjac2.javacompiler.code.DynamicGenerator;
import org.khelekore.parjac2.javacompiler.code.FieldGenerator;
import org.khelekore.parjac2.javacompiler.code.IfGenerator;
import org.khelekore.parjac2.javacompiler.code.IncrementGenerator;
import org.khelekore.parjac2.javacompiler.code.LocalVariableHandler;
import org.khelekore.parjac2.javacompiler.code.LoopGenerator;
import org.khelekore.parjac2.javacompiler.code.StringGenerator;
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
	CLASS_FLAGS (ClassFile.ACC_SUPER),
	ENUM_FLAGS (ClassFile.ACC_FINAL | ClassFile.ACC_ENUM),
	RECORD_FLAGS (ClassFile.ACC_FINAL | ClassFile.ACC_SUPER),
	INTERFACE_FLAGS (ClassFile.ACC_INTERFACE | ClassFile.ACC_ABSTRACT),
	ANNOTATION_FLAGS (ClassFile.ACC_ANNOTATION | ClassFile.ACC_INTERFACE),
	ENUM_CONSTANT_FLAGS (ClassFile.ACC_FINAL),
	ANONYMOUS_CLASS_FLAGS (ClassFile.ACC_SUPER);

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
	byte[] b = ClassFile.of().build (ClassDesc.of (name.getFullDollarName ()), classBuilder -> {
		classBuilder.withVersion (ClassFile.JAVA_21_VERSION, 0);  // possible minor: PREVIEW_MINOR_VERSION
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
	    case ExpressionName e -> runParts (partsToHandle, e.replaced ());
	    case FieldAccess fa -> fieldAccess (cb, fa);
	    case MethodInvocation mi -> methodInvocation (cb, mi);
	    case ReturnStatement r -> handleReturn (cb, r);
	    case UnaryExpression u -> handleUnaryExpression (cb, u);
	    case TwoPartExpression tp -> handleTwoPartExpression (cb, tp);
	    case Ternary t -> IfGenerator.handleTernary (this, cb, t);
	    case IfThenStatement ifts -> IfGenerator.handleIf (this, cb, ifts);
	    case Assignment a -> handleAssignment (cb, partsToHandle, a);
	    case LocalVariableDeclaration lv -> LocalVariableHandler.handleLocalVariables (this, cb, lv);
	    case PostIncrementExpression pie -> IncrementGenerator.handlePostIncrement (this, cb, cip.getFullName (td), pie);
	    case PostDecrementExpression pde -> IncrementGenerator.handlePostDecrement (this, cb, cip.getFullName (td), pde);
	    case BasicForStatement bfs -> LoopGenerator.handleBasicFor (this, cb, bfs);
	    case EnhancedForStatement efs -> LoopGenerator.handleEnhancedFor (this, cb, efs);
	    case SynchronizedStatement ss -> SynchronizationGenerator.handleSynchronized (this, cb, ss);
	    case ClassInstanceCreationExpression cic -> CodeUtil.callNew (cb, cic);

	    case ArrayCreationExpression ace -> ArrayGenerator.handleArrayCreation (this, cb, ace);
	    case ArrayAccess aa -> ArrayGenerator.handleArrayAccess (this, cb, aa);
	    case ArrayInitializer ai -> ArrayGenerator.handleArrayInitializer (this, cb, ai);

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


	private void fieldAccess (CodeBuilder cb, FieldAccess fa) {
	    cb.lineNumber (fa.position ().getLineNumber ()); // should be good enough
	    ParseTreeNode from = fa.from ();
	    VariableInfo vi = fa.variableInfo ();
	    if (vi.fieldType () == VariableInfo.Type.PARAMETER) {
		CodeUtil.loadParameter (cb, (FormalParameterBase)vi);
	    } else if (vi.fieldType () == VariableInfo.Type.LOCAL) {
		LocalVariable lv = (LocalVariable)vi;
		int slot = lv.vd ().slot ();
		FullNameHandler fn = FullNameHelper.type (lv.type ());
		TypeKind kind = FullNameHelper.getTypeKind (fn);
		cb.loadInstruction (kind, slot);
	    } else { // field
		FieldGenerator.getField (this, cb, from, cip.getFullName (td), vi);
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
	    DynamicGenerator.callDynamic (cb, name, flags, lambdaName, mtd, le.type (), false);
	    // TODO: this adds the lambda before the method we are currently building, consider queueing this up
	    addLambdaMethod (le, lambdaName, mtd);
	}

	private void callMethodReference (CodeBuilder cb, MethodReference mr) {
	    MethodInfo info = mr.methodInfo ();
	    boolean forceStatic = Flags.isStatic (mr.actualMethod ().flags ());
	    MethodTypeDesc mtd = info.methodTypeDesc ();
	    DynamicGenerator.callDynamic (cb, name, flags, mr.name (), mtd, mr.type (), forceStatic);
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
		StringGenerator.handleStringConcat (this, cb, two);
	    } else if (isArithmeticOrLogical (two)) {
		IfGenerator.handleTwoPartSetup (this, cb, two);
		mathOp (cb, two.fullName (), two.token ());
	    } else if (two.token () == javaTokens.LOGICAL_AND) {
		handleLogicalAnd (cb, two);
	    } else if (two.token () == javaTokens.LOGICAL_OR) {
		handleLogicalOr (cb, two);
	    } else if (two.token () == javaTokens.INSTANCEOF) {
		Label elseLabel = cb.newLabel ();
		IfGenerator.handleInstanceOf (this, cb, two, elseLabel);
		cb.labelBinding (elseLabel);
	    } else {
		Opcode jumpInstruction = IfGenerator.handleOtherTwoPart (this, cb, two);
		cb.ifThenElse (jumpInstruction, b -> b.iconst_1 (), b -> b.iconst_0 ());
	    }
	}

	private boolean isArithmeticOrLogical (TwoPartExpression two) {
	    return javaTokens.isArithmeticOrLogical (two.token ());
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
		TypeKind kind = FullNameHelper.getTypeKind (vi.typeName ());
		switch (vi.fieldType ()) {
		case VariableInfo.Type.FIELD ->
		    FieldGenerator.putField (this, cb, from, cip.getFullName (td), vi, value);
		case VariableInfo.Type.PARAMETER ->
		    putInLocalSlot (cb, kind, ((FormalParameterBase)vi).slot (), value);
		case VariableInfo.Type.LOCAL ->
		    putInLocalSlot (cb, kind, ((LocalVariable)vi).slot (), value);
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
		// TODO: implement better handling here, we currently generate
		// TODO: code that access the field twice, we could use a single dup instead.
		String newOp = name.substring (0, name.length () - 1);
		Token t = grammar.getExistingToken (newOp);
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

	@Override public Opcode getReverseZeroJump (Token t) {
	    if (t == javaTokens.DOUBLE_EQUAL) return Opcode.IFNE;
	    if (t == javaTokens.NOT_EQUAL) return Opcode.IFEQ;
	    if (t == javaTokens.LT) return Opcode.IFGE;
	    if (t == javaTokens.GT) return Opcode.IFLE;
	    if (t == javaTokens.LE) return Opcode.IFGT;
	    if (t == javaTokens.GE) return Opcode.IFLT;
	    throw new IllegalArgumentException ("Unknown zero comparisson: " + t);
	}

	@Override public Opcode getTwoPartJump (TwoPartExpression t) {
	    return getForwardJump (t.token (), t.optype () == TwoPartExpression.OpType.PRIMITIVE_OP);
	}

	@Override public Opcode getReverseTwoPartJump (TwoPartExpression t) {
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

	@Override public JavaTokens javaTokens () {
	    return javaTokens;
	}
    }

    private SignatureHelper.MethodSignatureHolder getMethodSignature (ConstructorDeclarationInfo c) {
	return SignatureHelper.getMethodSignature (cip, genericTypeHelper, c.getTypeParameters (), c.getFormalParameterList (), VOID_RETURN);
    }

    private SignatureHelper.MethodSignatureHolder getMethodSignature (MethodDeclarationBase m) {
	return SignatureHelper.getMethodSignature (cip, genericTypeHelper, m.getTypeParameters (), m.getFormalParameterList (), m.getResult ());
    }
}
