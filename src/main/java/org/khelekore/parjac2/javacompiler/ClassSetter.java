package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.AmbiguousName;
import org.khelekore.parjac2.javacompiler.syntaxtree.Annotation;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Assignment;
import org.khelekore.parjac2.javacompiler.syntaxtree.BasicForStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.BlockStatements;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassOrInterfaceTypeToInstantiate;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.DottedName;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnhancedForStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumConstant;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExceptionTypeList;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExpressionName;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterList;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.IfThenStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.ImportDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.LambdaExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodInvocation;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodReference;
import org.khelekore.parjac2.javacompiler.syntaxtree.NamePartHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalClassDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalInterfaceDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.PrimitiveType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ReceiverParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.RecordDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ReturnStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.SimpleClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.SingleStaticImportDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.SingleTypeImportDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.StaticImportOnDemandDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.Ternary;
import org.khelekore.parjac2.javacompiler.syntaxtree.ThisPrimary;
import org.khelekore.parjac2.javacompiler.syntaxtree.Throws;
import org.khelekore.parjac2.javacompiler.syntaxtree.TwoPartExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeArguments;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeBound;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeImportOnDemandDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeName;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameters;
import org.khelekore.parjac2.javacompiler.syntaxtree.UnqualifiedClassInstanceCreationExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclarator;
import org.khelekore.parjac2.javacompiler.syntaxtree.Wildcard;
import org.khelekore.parjac2.javacompiler.syntaxtree.WildcardBounds;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ClassSetter {

    private final JavaTokens javaTokens;
    private final ClassInformationProvider cip;
    private final ParsedEntry tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final String packageName;          // package name of the file we are looking at
    private final OrdinaryCompilationUnit ocu; // we do not care about ModularCompilationUnit (for now?)
    private final ImportHandler ih;            // keep track of the imports we have

    // cache so we do not have to recalculate them, key is MethodDeclarationBase or Constructor or Class
    private final Map<Object, EnclosingTypes> enclosureCache = new HashMap<> ();

    /** High level description:
     *  For each class:
     *      Setup a Scope
     *      Find fields, set type on them, add to scope of class
     *      Find methods, set type on arguments and add to scope of method
     *      Set type of expression
     */
    public static void fillInClasses (JavaTokens javaTokens,
				      ClassInformationProvider cip,
				      List<ParsedEntry> trees,
				      CompilerDiagnosticCollector diagnostics) {
	List<ClassSetter> classSetters =
	    trees.stream ()
	    .filter (pe -> (pe.getRoot () instanceof OrdinaryCompilationUnit))
	    .map (t -> new ClassSetter (javaTokens, cip, t, diagnostics)).collect (Collectors.toList ());
	if (diagnostics.hasError ())
	    return;
	classSetters.parallelStream ().forEach (ClassSetter::registerSuperTypes);

	classSetters.parallelStream ().forEach (ClassSetter::registerFields);
	classSetters.parallelStream ().forEach (ClassSetter::registerMethods);

	// now that we know what types, fields and methods we have we check the method contents
	classSetters.parallelStream ().forEach (ClassSetter::checkMethodBodies);

	classSetters.parallelStream ().forEach (cs -> cs.checkUnusedImport ());
    }

    public ClassSetter (JavaTokens javaTokens,
			ClassInformationProvider cip,
			ParsedEntry pe,
			CompilerDiagnosticCollector diagnostics) {
	this.javaTokens = javaTokens;
	this.cip = cip;
	tree = pe;
	this.diagnostics = diagnostics;

	ocu = (OrdinaryCompilationUnit)pe.getRoot ();
	packageName = ocu.getPackageName ();
	ih = new ImportHandler (ocu);
    }

    private void registerSuperTypes () {
	forAllTypes ((td, et) -> {
		switch (td) {
		case NormalClassDeclaration ndi -> registerSuperTypes (et, ndi);
		case EnumDeclaration ed -> setTypes (et, ed.getSuperInterfaces ());
		case NormalInterfaceDeclaration i -> registerSuperTypes (et, i);
		case RecordDeclaration rd -> setTypes (et, rd.getSuperInterfaces ());
		case UnqualifiedClassInstanceCreationExpression uc -> setType (et, uc.getSuperClass ());
		case EnumConstant ec -> setTypes (et, ec.getParent ().getSuperInterfaces ());
		default -> error (td, "ClassSetter.registerSuperTypes: Unhandled type: %s", td.getClass ().getName ());
		}
	    });
    }

    private void registerSuperTypes (EnclosingTypes et, NormalClassDeclaration ndi) {
	et = registerTypeParameters (et, ndi.getTypeParameters ());
	ClassType superclass = ndi.getSuperClass ();
	if (superclass != null)
	    setType (et, superclass);
	setTypes (et, ndi.getSuperInterfaces ());
    }

    private void registerSuperTypes (EnclosingTypes et, NormalInterfaceDeclaration i) {
	et = registerTypeParameters (et, i.getTypeParameters ());
	setTypes (et, i.getExtendsInterfaces ());
    }

    private EnclosingTypes registerTypeParameters (EnclosingTypes et, TypeParameters tps) {
	if (tps == null)
	    return et;
	Map<String, TypeParameter> nameToTypeParameter = new HashMap<> ();
	for (TypeParameter tp : tps.get ()) {
	    String id = tp.getId ();
	    TypeParameter previous = nameToTypeParameter.put (id, tp);
	    if (previous != null) {
		error (tp, "ClassSetter.registerTypeParameters: Type parameter name %s already declared", id);
	    }
	    // TODO: register the type parameter tp.getId ();
	    TypeBound bound = tp.getTypeBound ();
	    if (bound != null) {
		setType (et, bound.getType ());
		bound.getAdditionalBounds ().forEach (b -> setType (et, b));
	    }
	}
	return et.enclosingTypeParameter (nameToTypeParameter);
    }

    private void registerFields () {
	forAllTypes (this::setFieldTypes);
    }

    private void setFieldTypes (TypeDeclaration td, EnclosingTypes et) {
	EnclosingTypes ett = registerTypeParameters (et, td.getTypeParameters ());
	Map<String, FieldInfo> fields = td.getFields ();
	fields.forEach ((name, info) -> setFieldType (ett, name, info));
    }

    private void setFieldType (EnclosingTypes et, String name, FieldInfo info) {
	setType (et, info.type ());
    }

    private void registerMethods () {
	forAllTypes (this::registerMethods);
    }

    private void registerMethods (TypeDeclaration td, EnclosingTypes et) {
	EnclosingTypes ett = registerTypeParameters (et, td.getTypeParameters ());
	td.getMethods ().forEach (m -> setMethodTypes (ett, m));
	td.getConstructors ().forEach (c -> setConstructorTypes (ett, c));
	enclosureCache.put (td, ett);
    }

    private void setMethodTypes (EnclosingTypes et, MethodDeclarationBase md) {
	checkAnnotations (et, md.getAnnotations ());
	et = registerTypeParameters (et, md.getTypeParameters ());
	setType (et, md.getResult ());
	ReceiverParameter rp = md.getReceiverParameter ();
	if (rp != null) {
	    checkAnnotations (et, rp.getAnnotations ());
	    setType (et, rp.getType ());
	}
	et = setFormalParameterListTypes (et, md.getResult (), md.getFormalParameterList (), md.isStatic ());

	Throws t = md.getThrows ();
	if (t != null) {
	    ExceptionTypeList exceptions = t.getExceptions ();
	    setTypes (et, exceptions.get ());
	}

	enclosureCache.put (md, et);
    }

    private void setConstructorTypes (EnclosingTypes et, ConstructorDeclarationBase cdb) {
	checkAnnotations (et, cdb.getAnnotations ());
	et = registerTypeParameters (et, cdb.getTypeParameters ());
	et = setFormalParameterListTypes (et, null, cdb.getFormalParameterList (), false);
	enclosureCache.put (cdb, et);
    }

    private EnclosingTypes setFormalParameterListTypes (EnclosingTypes et, ParseTreeNode result, FormalParameterList args, boolean isStatic) {
	Map<String, VariableInfo> variables = new HashMap<> ();
	if (args != null) {
	    int i = 0;
	    for (FormalParameterBase fp : args.getParameters ()) {
		fp.slot (i++);
		checkFormalParameterModifiers (et, fp.getModifiers ());
		setType (et, fp.type ());
		String id = fp.name ();
		VariableInfo previous = variables.put (id, fp);
		if (previous != null) {
		    error (fp, "ClassSetter.setFormalParameterListTypes: name %s already in use", id);
		}
	    }
	}
	return et.enclosingMethod (result, variables, isStatic);
    }

    private void checkFormalParameterModifiers (EnclosingTypes et, List<ParseTreeNode> modifiers) {
	for (ParseTreeNode n : modifiers) {
	    if (n instanceof Annotation a)
		setType (et, a.getTypeName ());
	}
    }

    private void checkMethodBodies () {
	forAllTypes (this::checkMethodBodies);
    }

    private void checkMethodBodies (TypeDeclaration td, EnclosingTypes et) {
	EnclosingTypes ett = enclosureCache.get (td);
	td.getMethods ().forEach (m -> checkMethodBodies (m));
	td.getConstructors ().forEach (c -> checkConstructorBodies (c));
	td.getInstanceInitializers ().forEach (c -> checkInstanceInitializerBodies (ett, c));
	td.getStaticInitializers ().forEach (c -> checkStaticInitializerBodies (ett, c));
    }

    private void checkMethodBodies (MethodDeclarationBase md) {
	EnclosingTypes et = enclosureCache.get (md);
	ParseTreeNode body = md.getMethodBody ();
	if (body instanceof Block block) {
	    et = et.enclosingBlock (false);
	    BlockStatements bs = block.getStatements ();
	    if (bs != null)
		setTypesForMethodStatement (et, bs.getStatements ());
	}
    }

    private void checkConstructorBodies (ConstructorDeclarationBase cdb) {
	EnclosingTypes et = enclosureCache.get (cdb);
	et = et.enclosingBlock (false);
	setTypesForMethodStatement (et, cdb.statements ());
    }

    private void checkInstanceInitializerBodies (EnclosingTypes et, ParseTreeNode p) {
	et = et.enclosingBlock (false);
	setTypesForMethodStatement (et, List.of (p));
    }

    private void checkStaticInitializerBodies (EnclosingTypes et, ParseTreeNode p) {
	et = et.enclosingBlock (true);
	setTypesForMethodStatement (et, List.of (p));
    }

    private void setTypesForMethodStatement (EnclosingTypes initial, List<ParseTreeNode> ps) {
	Deque<StatementHandler> partsToHandle = new ArrayDeque<> ();
	ps.forEach (n -> partsToHandle.add (new StatementHandler (initial, n)));
	while (!partsToHandle.isEmpty ()) {
	    StatementHandler s = partsToHandle.removeFirst ();
	    Object pp = s.handler;
	    EnclosingTypes et = s.et;
	    // quite useful when debugging
	    //System.err.println ("Looking at: " + pp + ", " + pp.getClass ().getSimpleName () + ", " + et.enclosure ().getClass ().getSimpleName ());
	    switch (pp) {
	    case Block b -> runInBlock (et, b, partsToHandle);
	    case LambdaExpression le -> runInLambda (et, le, partsToHandle);
	    case PrimitiveType pt -> setType (et, pt);
	    case ClassType ct -> setType (et, ct);
	    case AmbiguousName an -> reclassifyAmbiguousName (et, an);
	    case ExpressionName en -> reclassifyAmbiguousName (et, en);
	    case DottedName dt -> setType (et, dt);
	    case MethodReference mr -> {
		// skip for now
	    }
	    case ThisPrimary tp -> setType (et, tp);
	    case FieldAccess fa -> handlePartsAndField (et, fa, partsToHandle);
	    case ClassOrInterfaceTypeToInstantiate coitti -> setType (et, coitti.type ());

	    case VariableDeclarator vd -> handlePartsAndVariableDeclarator (et, vd, partsToHandle);
	    case Assignment a -> handlePartsAndHandler (et, a, e -> checkAssignment (e, a), partsToHandle);

	    // Check method once we know all parts
	    case MethodInvocation mi -> handleMethodInvocation (et, mi, partsToHandle);
	    case Ternary t -> handlePartsAndHandler (et, t, e -> setTernaryType (e, t), partsToHandle);
	    case TwoPartExpression t -> handlePartsAndHandler (et, t, e -> setTwoPartExpressionType (e, t), partsToHandle);
	    case ReturnStatement r -> handlePartsAndHandler (et, r, e -> setReturnStatementType (e, r), partsToHandle);
	    case IfThenStatement i -> handlePartsAndHandler (et, i, e -> checkIfExpressionType (e, i), partsToHandle);
	    case BasicForStatement bfs -> handlePartsAndHandler (et, bfs, e -> checkBasicForExpression (e, bfs), partsToHandle);
	    case EnhancedForStatement efs -> handlePartsAndHandler (et, efs, e -> checkEnhancedForExpression (e, efs), partsToHandle);

	    case ParseTreeNode ptn -> addParts (et, ptn, partsToHandle);

	    case CustomHandler ch -> ch.run (et);

	    default -> throw new IllegalStateException ("Unhandled type: " + pp);
	    }
	}
    }

    private record StatementHandler (EnclosingTypes et, Object handler) {
	// empty
    }

    private void runInBlock (EnclosingTypes et, Block b, Deque<StatementHandler> partsToHandle) {
	EnclosingTypes bt = et.enclosingBlock (et.isStatic ());
	addParts (bt, b, partsToHandle);
    }

    private void runInLambda (EnclosingTypes et, LambdaExpression le, Deque<StatementHandler> partsToHandle) {
	EnclosingTypes bt = et.enclosingBlock (false);
	EnclosingTypes.BlockEnclosure bc = (EnclosingTypes.BlockEnclosure)et.enclosure ();
	for (int i = 0, s = le.numberOfArguments (); i < s; i++)
	    bc.add (new LambdaVarInfo (le, i));

	partsToHandle.addFirst (new StatementHandler (bt, le.body ()));
	partsToHandle.addFirst (new StatementHandler (et, le.params ()));
    }

    private record LambdaVarInfo (LambdaExpression le, int paramPos) implements VariableInfo {
	@Override public Type fieldType () {
	    return VariableInfo.Type.LOCAL;
	}

	@Override public int flags () {
	    return Flags.ACC_PUBLIC;
	}

	@Override public String name () {
	    return le.parameterName (paramPos);
	}

	@Override public FullNameHandler typeName () {
	    return le.parameter (paramPos);
	}
    }

    private void handlePartsAndField (EnclosingTypes et, FieldAccess fa, Deque<StatementHandler> partsToHandle) {
	CustomHandler h = e -> setType (e, fa);
	partsToHandle.addFirst (new StatementHandler (et, h));
	addParts (et, fa, partsToHandle);
    }

    private void handlePartsAndVariableDeclarator (EnclosingTypes et, VariableDeclarator vd, Deque<StatementHandler> partsToHandle) {
	partsToHandle.addFirst (new StatementHandler (et, new AddVariable (vd)));
	FullNameHandler varType = FullNameHelper.type (vd.type ());
	ParseTreeNode init = vd.initializer ();
	if (init instanceof LambdaExpression le) {
	    setTypeOnLambda (varType, le);
	    addLambdaReturnCheck (et, le, partsToHandle);
	    partsToHandle.addFirst (new StatementHandler (et, le));
	} else if (init != null) {
	    CustomHandler h = e -> checkInitializerType (varType, vd.initializer ());
	    partsToHandle.addFirst (new StatementHandler (et, h));
	    partsToHandle.addFirst (new StatementHandler (et, init));
	}
    }

    private void checkInitializerType (FullNameHandler varType, ParseTreeNode initializer) {
	if (initializer != null) {
	    if (initializer instanceof LambdaExpression le) {
		// already handled
	    } else {
		FullNameHandler fi = FullNameHelper.type (initializer);
		// if varType == null, we have already signaled type not found or similar
		// If fi == null we have already signaled errors and do not want another one here.
		if (varType != null && fi != null && !typesMatch (varType, fi))
		    error (initializer, "Types not compatible: %s <-> %s", varType.getFullDotName (), fi.getFullDotName ());
	    }
	}
    }

    private void checkAssignment (EnclosingTypes et, Assignment a) {
	FullNameHandler toType = FullNameHelper.type (a.lhs ());
	FullNameHandler fromType = FullNameHelper.type (a.rhs ());
	if (toType != null && fromType != null && !typesMatch (toType, fromType))
	    error (a, "Types not compatible: %s <-> %s", toType.getFullDotName (), fromType.getFullDotName ());
    }

    private void handleMethodInvocation (EnclosingTypes et, MethodInvocation mi, Deque<StatementHandler> partsToHandle) {
	// We can not check lambda contents before we know the parameter types of the lambda.
	// We only know the types after we have found a method match
	// So this is the reason we delay the lambda checks to run at the end of checkMethodCall
	List<LambdaExpression> lambdas = new ArrayList<> ();
	List<ParseTreeNode> toCheck = new ArrayList<> ();
	addNonNull (mi.getOn (), toCheck);
	addNonNull (mi.types (), toCheck);
	for (ParseTreeNode a : mi.getArguments ()) {
	    if (a instanceof LambdaExpression le)
		lambdas.add (le);
	    else
		toCheck.add (a);
	}

	CustomHandler h = e -> checkMethodCall (e, mi, lambdas, partsToHandle);
	partsToHandle.addFirst (new StatementHandler (et, h));
	addParts (et, toCheck, partsToHandle);
    }

    private void addNonNull (ParseTreeNode p, List<ParseTreeNode> ls) {
	if (p != null)
	    ls.add (p);
    }

    private void setTypeOnLambda (FullNameHandler varType, LambdaExpression le) {
	MethodInfo mi = lambdaMatch (varType, le);
	if (mi != null) {
	    le.type (varType, mi);
	} else {
	    error (le, "Lambda expression not assignable to %s", varType.getFullDotName ());
	}
    }

    private void addLambdaReturnCheck (EnclosingTypes et, LambdaExpression le, Deque<StatementHandler> partsToHandle) {
	CustomHandler h = e -> checkLambdaReturn (le);
	partsToHandle.addFirst (new StatementHandler (et, h));
    }

    private void checkLambdaReturn (LambdaExpression le) {
	FullNameHandler miR = le.result ();
	if (miR == FullNameHandler.VOID && !(le.body () instanceof Block))
	    return;
	FullNameHandler leR = le.lambdaResult ();
	if (!miR.equals (leR))
	    error (le, "Wrong type returned from lambda: %s, need: %s", leR.getFullDotName (), miR.getFullDotName ());
    }

    private void handlePartsAndHandler (EnclosingTypes et, ParseTreeNode p,
					CustomHandler h, Deque<StatementHandler> partsToHandle) {
	partsToHandle.addFirst (new StatementHandler (et, h));
	addParts (et, p, partsToHandle);
    }

    private void addParts (EnclosingTypes et, ParseTreeNode pp, Deque<StatementHandler> partsToHandle) {
	addParts (et, pp.getChildren (), partsToHandle);
    }

    private <T extends ParseTreeNode> void addParts (EnclosingTypes et, List<T> parts, Deque<StatementHandler> partsToHandle) {
	for (int i = parts.size () - 1; i >= 0; i--) {
	    partsToHandle.addFirst (new StatementHandler (et, parts.get (i)));
	}
    }

    /* TODO: rewrite this code a bit: we should only call an.replace and an.fullName when we are done
     */
    private void reclassifyAmbiguousName (EnclosingTypes et, DottedName an) {
	String start = an.getNamePart (0);
	VariableOrError voe = getVariable (et, start);
	if (voe.error != null) {
	    error (an, voe.error);
	    return;
	}
	tryToSetFullNameOnSimpleName (et, an, start, voe.vi);

	for (int i = 1; i < an.size (); i++) {
	    FullNameHandler fn = an.fullName ();
	    String id = an.getNamePart (i);
	    if (fn == null) { // (part of) package name
		// since name is fully specified we do not want to check outer enclosures
		FullNameHandler currentName = an.dotName (i);
		FullNameHandler fqn = getVisibleType (null, currentName);
		if (fqn != null)
		    an.fullName (fqn);
	    } else if (fn instanceof FullNameHandler.ArrayHandler ah) {
		if (id.equals ("length")) {
		    FullNameHandler inner = FullNameHandler.INT;
		    an.fullName (inner);
		    FieldAccess fa = new FieldAccess (an.position (), an.replaced (), id);
		    an.replace (fa);
		} else {
		    error (an, "No field: %s for array type", id);
		    an.fullName (null);
		    return;
		}
	    } else if (fn instanceof FullNameHandler.Primitive pt) {
		// TODO: Most of auto boxing happens in code generation, but we may need to handle here as well
		error (an, "No field: %s for primitive type", id);
		an.fullName (null);
		return;
	    } else { // we have a type
		VariableInfo fi = cip.getFieldInformation (fn, id);
		if (fi != null) {
		    if (an.replaced () instanceof ClassType) {
			if (!Flags.isStatic (fi.flags ())) {
			    error (an, nonStaticAccess (id));
			    an.fullName (null);
			    return;
			}
		    }
		    // if we find a field we can not access we can not log an error since doing so will
		    // result in us reporting multiple errors. We have to rely on the "unable to find symbol" from above.
		    if (isAccessible (et, fn, fi)) {
			an.fullName (fi.typeName ());
			// TODO: probably need to handle ExpressionName as well
			FieldAccess fa = new FieldAccess (an.position (), an.replaced (), id);
			fa.variableInfo (fi);
			an.replace (fa);
		    }
		} else {
		    FullNameHandler innerClassCandidate = fn.getInnerClass (id);
		    FullNameHandler foundInnerClass = getVisibleType (null, innerClassCandidate);
		    if (foundInnerClass != null)
			an.fullName (foundInnerClass);
		}
	    }
	}
	if (an.fullName () == null)
	    error (an, "Unable to find symbol: %s", an.getDotName ());
    }

    private void tryToSetFullNameOnSimpleName (EnclosingTypes et, DottedName an, String name, VariableInfo fi) {
	// We do not need to test for accessible, we asked for a field in our enclosure
	// TODO: validate this, what about private field in superclass?
	if (fi != null) { // known variable
	    FieldAccess access = new FieldAccess (an.position (), null, name);
	    an.replace (access);
	    FullNameHandler fn = fi.typeName ();
	    an.fullName (fn);
	    access.variableInfo (fi);
	} else {
	    FullNameHandler fn = FullNameHandler.ofSimpleClassName (name);
	    ResolvedClass fqn = resolve (et, fn, an.position ());
	    if (fqn != null) {
		an.fullName (fqn.type);
		an.replace (new ClassType (fqn.type));
	    }
	}
    }

    private VariableOrError getVariable (EnclosingTypes et, String name) {
	boolean onlyStatic = false;
	while (et != null) {
	    VariableInfo fi = findField (et, name);
	    if (fi != null) {
		if (!onlyStatic || Flags.isStatic (fi.flags ()))
		    return new VariableOrError (fi, null);
		return new VariableOrError (null, nonStaticAccess (name));
	    }
	    onlyStatic |= et.isStatic ();
	    et = et.previous ();
	}
	return new VariableOrError (null, null); // not found, but also not an error
    }

    private VariableInfo findField (EnclosingTypes et, String name) {
	return getField (n -> et.enclosure ().getField (n), () -> et.getSuperClasses (cip), name);
    }

    private String nonStaticAccess (String name) {
	return String.format ("non-static variable %s cannot be referenced from a static context", name);
    }

    private record VariableOrError (VariableInfo vi, String error) {
	// empty
    }

    private static record AddVariable (VariableDeclarator vd) implements CustomHandler {
	@Override public void run (EnclosingTypes et) {
	    ((EnclosingTypes.BlockEnclosure)et.enclosure ()).add (vd);
	}
    }

    private void checkMethodCall (EnclosingTypes et, MethodInvocation mi, List<LambdaExpression> les, Deque<StatementHandler> partsToHandle) {
	EnclosingTypes currentBlock = et;
	ParseTreeNode on = mi.getOn ();
	boolean isSuper = mi.isSuper ();
	String name = mi.getMethodName ();
	List<ParseTreeNode> args = mi.getArguments ();

	FullNameHandler methodOn = null;
	if (on != null) {
	    methodOn = FullNameHelper.type (on);

	    // AmbiguousName that could not be resolved and similar, no need for NPE or another error
	    if (methodOn == null)
		return;
	}
	if (isSuper) {
	    // TODO: set methodOn
	}

	// useful when debugging
	//System.err.println ("Trying to check method: " + mi + ", currently inside: " + currentClass (et).getFullDotName ());
	boolean insideStatic = false;
	while (et != null) {
	    if (et.enclosure () instanceof EnclosingTypes.TypeEnclosure) {
		if (on == null) {
		    methodOn = currentClass (et);
		}

		MethodInfo info = getMethod (mi, methodOn, on, name, args, insideStatic);
		if (info != null) {
		    mi.info (info);
		    break;
		}
	    }
	    insideStatic |= et.isStatic ();
	    et = et.previous ();
	}
	if (mi.info () == null) {
	    // useful when debugging
	    //System.err.println ("Failed to find method: " + name);
	    error (mi, "No matching method named %s found in %s", name, methodOn.getFullDotName ());
	}

	// now that we know the type of the lambdas we can check them.
	addParts (currentBlock, les, partsToHandle);
    }

    private MethodInfo getMethod (MethodInvocation mi, FullNameHandler methodOn,
				  ParseTreeNode on, String name, List<ParseTreeNode> args, boolean insideStatic) {
	MethodInfo info = getMatching (cip.getMethods (methodOn, name), on, args, insideStatic);
	if (info == null) {
	    List<FullNameHandler> supers = getSuperClasses (methodOn);
	    if (supers != null) {
		for (FullNameHandler sfn : supers) {
		    info = getMatching (cip.getMethods (sfn, name), on, args, insideStatic);
		    if (info != null)
			break;
		}
	    }
	}
	return info;
    }

    private MethodInfo getMatching (List<MethodInfo> options, ParseTreeNode on, List<ParseTreeNode> args, boolean insideStatic) {
	for (MethodInfo info : options) {
	    if (match (on, args, info, insideStatic))
		return info;
	}
	return null;
    }

    private boolean match (ParseTreeNode on, List<ParseTreeNode> args, MethodInfo info, boolean insideStatic) {
	if (on instanceof DottedName dn)
	    on = dn.replaced ();
	boolean requireInstance = !Flags.isStatic (info.flags ());
	boolean varArgs = Flags.isVarArgs (info.flags ());
	if (!varArgs && args.size () != info.numberOfArguments ())
	    return false;

	if (requireInstance) {
	    // Object.equals (foo)
	    if (on instanceof ClassType) {
		return false;
	    }
	    // we can not use implicit or explicit "this" inside static method
	    if (insideStatic && (on == null || on instanceof ThisPrimary)) {
		return false;
	    }
	}

	FullNameHandler lastVarArg = null;
	if (varArgs) {
	    lastVarArg = info.parameter (info.numberOfArguments () - 1);
	    FullNameHandler.ArrayHandler ah = (FullNameHandler.ArrayHandler)lastVarArg;
	    lastVarArg = ah.inner ();
	}
	Map<LambdaExpression, Runnable> lambdaTypes = new HashMap<> ();
	for (int i = 0; i < args.size (); i++) {
	    FullNameHandler pfn;
	    if (varArgs && i >= info.numberOfArguments () - 1) {
		pfn = lastVarArg;
	    } else {
		pfn = info.parameter (i);
	    }

	    ParseTreeNode pa = args.get (i);
	    FullNameHandler afn;
	    if (pa instanceof LambdaExpression le) {
		MethodInfo mi = lambdaMatch (pfn, le);
		if (mi != null) {
		    afn = pfn;
		    lambdaTypes.put (le, () -> le.type (pfn, mi));
		} else {
		    return false;
		}
	    } else {
		afn = FullNameHelper.type (pa);
	    }
	    // Useful when debugging
	    //System.err.println ("    " +i + ", args(" + i + "): " + pa + " -> " + dotName (afn) + ", pfn: "+ dotName (pfn));
	    if (!typesMatch (pfn, afn))
		return false;
	}

	// we only set the types if we actually found a match.
	lambdaTypes.values ().forEach (r -> r.run ());
	lambdaTypes.keySet ().forEach (le -> checkLambdaReturn (le));
	return true;
    }

    private boolean isAccessible (EnclosingTypes et, FullNameHandler fqn, VariableInfo fi) {
	FullNameHandler topLevelClass = et == null ? null : lastFQN (et);
	return isAccessible (et, fqn, topLevelClass, fi.flags ());
    }

    private void setTernaryType (EnclosingTypes et, Ternary t) {
	FullNameHandler thenType = FullNameHelper.type (t.thenPart ());
	FullNameHandler elseType = FullNameHelper.type (t.elsePart ());
	t.type (commonType (thenType, elseType));
    }

    private FullNameHandler commonType (FullNameHandler f1, FullNameHandler f2) {
	if (f1.equals (f2))
	    return f1;

	// TODO: implement this correctly
	return f1;
    }

    private void setTwoPartExpressionType (EnclosingTypes et, TwoPartExpression t) {
	FullNameHandler part1 = FullNameHelper.type (t.part1 ());
	FullNameHandler part2 = FullNameHelper.type (t.part2 ());
	if (part1 == null || part2 == null) // missing thingy, reported elsewhere
	    return;
	Token token = t.token ();
	if (part1.isPrimitive () && part2.isPrimitive ()) {
	    if (isComparisson (t.token ()))
		t.fullName (FullNameHandler.BOOLEAN);
	    else
		t.fullName (FullNameHelper.wider (part1, part2));
	    t.optype (TwoPartExpression.OpType.PRIMITIVE_OP);
	} else if ((part1.equals (FullNameHandler.JL_STRING) || part2.equals (FullNameHandler.JL_STRING)) &&
		   isStringConcat (token)) {
	    t.fullName (FullNameHandler.JL_STRING);
	    t.optype (TwoPartExpression.OpType.STRING_OP);
	} else if (isObjectComparisson (token)) {
	    t.fullName (FullNameHandler.BOOLEAN);
	    t.optype (TwoPartExpression.OpType.OBJECT_OP);
	} else {
	    error (t, "Unhandled type in two part expression: %t: (%s, %s)", t, part1.getFullDotName (), part2.getFullDotName ());
	}
    }

    private boolean isComparisson (Token t) {
	return javaTokens.isComparisson (t);
    }

    private boolean isStringConcat (Token t) {
	return t == javaTokens.PLUS;
    }

    private boolean isObjectComparisson (Token t) {
	return t == javaTokens.DOUBLE_EQUAL || t == javaTokens.NOT_EQUAL;
    }

    private void setReturnStatementType (EnclosingTypes et, ReturnStatement r) {
	ParseTreeNode methodReturn = currentMethodReturn (et);
	FullNameHandler fm = FullNameHelper.type (methodReturn);
	ParseTreeNode p = r.expression ();
	FullNameHandler fr = p == null ? FullNameHandler.VOID : FullNameHelper.type (p);
	if (fr == null)  // if we have failed to figure out the type we have already reported it.
	    return;
	if (!typesMatch (fm, fr)) {
	    error (r, "Return statement (%s) incompatible with method return type (%s)", fr.getFullDotName (), fm.getFullDotName ());
	}
	r.type (fm);
    }

    private void checkIfExpressionType (EnclosingTypes et, IfThenStatement i) {
	checkTest (et, i.test ()); // null not allowed
    }

    private void checkBasicForExpression (EnclosingTypes et, BasicForStatement bfs) {
	ParseTreeNode p = bfs.expression ();
	if (p != null)
	    checkTest (et, p);
    }

    private void checkEnhancedForExpression (EnclosingTypes et, EnhancedForStatement efs) {
	ParseTreeNode exp = efs.expression ();
	FullNameHandler fn = FullNameHelper.type (exp);
	if (!(fn.isArray () || isIterable (fn)))
	    error (exp, "Expression in for-each must be Iterable, current type: " + fn.getFullDotName ());
    }

    private boolean isIterable (FullNameHandler fn) {
	Set<FullNameHandler> allSupers = getAllSuperTypes (fn);
	return allSupers.contains (FullNameHandler.JL_ITERABLE);
    }

    private void checkTest (EnclosingTypes et, ParseTreeNode test) {
	String type = "<missing>";
	if (test != null) {
	    FullNameHandler fn = FullNameHelper.type (test);
	    // TODO: useful when debugging
	    /*
	    if (fn == null)
		System.err.println ("test is: " + test + ", " + currentClass (et).getFullDotName ());
	    */
	    if (fn == FullNameHandler.BOOLEAN)
		return;
	    type = fn.getFullDotName ();
	}
	error (test, "Test needs to evaluate to boolean value, current type: %s", type);
    }

    private MethodInfo lambdaMatch (FullNameHandler type, LambdaExpression le) {
	MethodInfo mi = getFunctionalInterfaceMethod (type);
	if (mi == null)
	    return null;

	int s = mi.numberOfArguments ();
	int t = le.numberOfArguments ();
	if (s != t)
	    return null;
	for (int i = 0; i < s; i++) {
	    FullNameHandler ff = mi.parameter (i);
	    FullNameHandler fl = le.lambdaParameter (i);
	    if (fl != null && !fl.equals (ff))
		return null;
	}
	return mi;
    }

    private MethodInfo getFunctionalInterfaceMethod (FullNameHandler type) {
	String fqn  = type.getFullDotName ();
	if (!cip.isInterface (fqn))
	    return null;
	Map<String, List<MethodInfo>> methods = cip.getMethods (type);
	Set<MethodInfo> abstracts = new HashSet<> ();
	for (List<MethodInfo> ls : methods.values ())
	    for (MethodInfo mi : ls)
		if (Flags.isAbstract (mi.flags ()))
		    abstracts.add (mi);
	if (abstracts.size () > 1)
	    return null;
	MethodInfo mi = abstracts.iterator ().next ();
	Map<String, List<MethodInfo>> objectMethods = cip.getMethods (FullNameHandler.JL_OBJECT);
	if (!hasMatching (objectMethods, mi))
	    return mi;
	return null;
    }

    private boolean hasMatching (Map<String, List<MethodInfo>> mis, MethodInfo mi) {
	for (List<MethodInfo> ls : mis.values ()) {
	    for (MethodInfo mii : ls) {
		if (matching (mii, mi))
		    return true;
	    }
	}
	return false;
    }

    private boolean matching (MethodInfo m1, MethodInfo m2) {
	int s = m1.numberOfArguments ();
	if (s != m2.numberOfArguments ())
	    return false;
	if (!m1.name ().equals (m2.name ()))
	    return false;
	for (int i = 0; i < s; i++) {
	    FullNameHandler f1 = m1.parameter (i);
	    FullNameHandler f2 = m1.parameter (i);
	    if (!f1.equals (f2))
		return false;
	}
	FullNameHandler r1 = m1.result ();
	FullNameHandler r2 = m2.result ();
	return r1.equals (r2);
    }

    private boolean typesMatch (FullNameHandler wanted, FullNameHandler have) {
	if (wanted == null || have == null)
	    return false;
	if (have.equals (wanted))
	    return true;
	if (wanted.isArray ()) {
	    if (!have.isArray ())
		return false;
	    // both arrays
	    FullNameHandler.ArrayHandler wantedA = (FullNameHandler.ArrayHandler)wanted;
	    FullNameHandler.ArrayHandler haveA = (FullNameHandler.ArrayHandler)have;
	    if (isSuperClass (wantedA.inner (), haveA.inner ()))
		return true;
	} else if (wanted.isPrimitive ()) {
	    if (FullNameHelper.mayAutoCastPrimitives (have, wanted)) // widening
		return true;
	    if (FullNameHelper.canAutoUnBoxTo (have, wanted))
		return true;
	} else {
	    if (wanted.getType () == FullNameHandler.Type.OBJECT && have == FullNameHandler.NULL)
		return true;
	    if (have.isArray ())
		return wanted == FullNameHandler.JL_OBJECT;
	    if (FullNameHelper.canAutoBoxTo (have, wanted))
		return true;
	    if (isSuperClass (wanted, have))
		return true;
	    if (isAutoboxSuperClass (wanted, have))
		return true;
	}
	return false;
    }

    private boolean isAutoboxSuperClass (FullNameHandler supertype, FullNameHandler subtype) {
	FullNameHandler ab = FullNameHelper.getAutoBoxOption (subtype);
	if (ab != null && isSuperClass (supertype, ab))
	    return true;
	return false;
    }

    private boolean isSuperClass (FullNameHandler supertype, FullNameHandler subtype) {
	Set<FullNameHandler> allSuperTypes = getAllSuperTypes (subtype);
	return allSuperTypes.contains (supertype);
    }

    private Set<FullNameHandler> getAllSuperTypes (FullNameHandler subtype) {
	Set<FullNameHandler> ret = new HashSet<> ();
	Deque<FullNameHandler> d = new ArrayDeque<> ();
	d.add (subtype);
	while (!d.isEmpty ()) {
	    FullNameHandler s = d.removeFirst ();
	    List<FullNameHandler> supers = getSuperClasses (s);
	    if (supers != null) {
		for (FullNameHandler f : supers) {
		    if (ret.add (f)) {
			d.addLast (f);
		    }
		}
	    }
	}
	return ret;
    }

    private ParseTreeNode currentMethodReturn (EnclosingTypes et) {
	while (et != null) {
	    if (et.enclosure () instanceof EnclosingTypes.MethodEnclosure me)
		return me.returnType ();
	    et = et.previous ();
	}
	throw new IllegalStateException ("Unable to find method enclosure");
    }

    private interface CustomHandler {
	void run (EnclosingTypes et);
    }

    private void checkAnnotations (EnclosingTypes et, List<? extends ParseTreeNode> annotations) {
	if (annotations == null)
	    return;
	for (ParseTreeNode t : annotations) {
	    switch (t) {
	    case Annotation a -> setType (et, a.getTypeName ());
	    default -> error (t, "ClassSetter: Unhandled annotation: %s, %s", t.getClass ().getName (), t);
	    }
	}
    }

    private void forAllTypes (BiConsumer<TypeDeclaration, EnclosingTypes> handler) {
	Deque<EnclosingTypes> typesToHandle = new ArrayDeque<> ();
	ocu.getTypes ().stream ().map (td -> EnclosingTypes.topLevel (td, cip)).forEach (typesToHandle::add);
	while (!typesToHandle.isEmpty ()) {
	    EnclosingTypes et = typesToHandle.removeFirst ();
	    TypeDeclaration td = et.td ();
	    handler.accept (td, et);
	    td.getInnerClasses ().stream ().map (i -> et.enclosingTypes (i, cip)).forEach (typesToHandle::add);
	}
    }

    private void setType (EnclosingTypes et, ParseTreeNode type) {
	switch (type) {
	case TokenNode t -> { /* empty */ }
	case PrimitiveType p -> setType (et, p);
	case ClassType ct -> setType (et, ct);
	case DottedName dn -> setType (et, dn);
	case ArrayType at -> setType (et, at.getType ());
	case Wildcard wc -> {
	    WildcardBounds wb = wc.getBounds ();
	    if (wb != null)
		setType (et, wb.getClassType ());
	}
	default -> error (type, "ClassSetter: Unhandled type: %s, %s", type.getClass ().getName (), type);
	}
    }

    private void setTypes (EnclosingTypes et, List<ClassType> types) {
	if (types != null)
	    types.forEach (ct -> setType (et, ct));
    }

    private void setType (EnclosingTypes et, PrimitiveType pt) {
	if (pt.hasFullName ()) // already set?
	    return;
	pt.fullName (FullNameHelper.getPrimitive (pt.type ()));
    }

    private void setType (EnclosingTypes et, ClassType ct) {
	if (ct.hasFullName ()) // already set?
	    return;
	// "List" and "java.util.List" are easy to resolve
	FullNameHandler id = ct.getFullNameAsSimpleDottedName ();
	ResolvedClass fqn = resolve (et, id, ct.position ());
	if (fqn == null && ct.size () > 1) {
	    // ok, someone probably wrote something like "HashMap.Entry" or
	    // "java.util.HashMap.Entry" which is somewhat problematic, but legal
	    fqn = tryAllParts (et, ct, ct.position ());
	}
	if (fqn == null) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.position (),
							 "Failed to find class for type: %s", id));
	    return;
	}
	ct.fullName (fqn.type);
	ct.setTypeParameter (fqn.tp);
	for (SimpleClassType sct : ct.get ()) {
	    TypeArguments tas = sct.getTypeArguments ();
	    if (tas != null) {
		tas.getTypeArguments ().forEach (tn -> setType (et, tn));
	    }
	}
    }

    private void setType (EnclosingTypes et, DottedName tn) {
	if (tn.hasFullName ())
	    return;
	FullNameHandler id = tn.getFullNameAsSimpleDottedName ();
	ResolvedClass fqn = resolve (et, id, tn.position ());
	if (fqn == null && tn.size () > 1) {
	    fqn = tryAllParts (et, tn, tn.position ());
	}
	if (fqn == null) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), tn.position (),
							 "Failed to find class for dotted name: %s", id));
	    return;
	}
	tn.fullName (fqn.type);
    }

    private void setType (EnclosingTypes et, ThisPrimary tp) {
	tp.type (currentClass (et));
    }

    private void setType (EnclosingTypes et, FieldAccess fa) {
	FullNameHandler fn = currentClass (et);
	ParseTreeNode from = fa.from ();
	if (from != null) {
	    if (!(from instanceof ThisPrimary))
		fn = FullNameHelper.type (from);
	}

	String id = fa.name ();
	VariableInfo fi = getField (fn, id);
	if (fi != null) {
	    fa.variableInfo (fi);
	} else {
	    error (fa, "Unable to find field: %s in type: %s", id, fn.getFullDotName ());
	}
    }

    private VariableInfo getField (FullNameHandler fn, String name) {
	return getField (n -> cip.getFieldInformation (fn, n), () -> getSuperClasses (fn), name);
    }

    private VariableInfo getField (Function<String, VariableInfo> fieldGetter,
				   Supplier<List<FullNameHandler>> superClassesGetter,
				   String name) {
	VariableInfo fi = fieldGetter.apply (name);
	if (fi == null) {
	    List<FullNameHandler> supers = superClassesGetter.get ();
	    if (supers != null) {
		for (FullNameHandler sfn : supers) {
		    fi = cip.getFieldInformation (sfn, name);
		    if (fi != null)
			break;
		}
	    }
	}
	return fi;
    }

    private class ImportHandler {
	private final Map<String, ImportHolder> stid = new HashMap<> ();
	private final List<ImportHolder> tiod = new ArrayList<> ();
	private final Map<String, StaticImportType> ssid = new HashMap<> ();
	private final List<StaticImportOnDemandDeclaration> siod = new ArrayList<> ();

	public ImportHandler (OrdinaryCompilationUnit tree) {
	    tiod.add (new ImportHolder (null, "java.lang"));
	    for (ImportDeclaration id : tree.getImports ()) {
		switch (id) {
		case SingleTypeImportDeclaration stid -> addSingleTypeImportDeclaration (stid);
		case TypeImportOnDemandDeclaration i -> tiod.add (new ImportHolder (i, i.getName ().getDotName ()));
		case SingleStaticImportDeclaration ssid -> addSingleStaticImportDeclaration (ssid);
		case StaticImportOnDemandDeclaration i -> siod.add (i);
		}
	    }
	}

	@Override public String toString () {
	    return "imports: " + stid + "\n" + tiod + "\n" + ssid + "\n" + siod;
	}

	private void addSingleTypeImportDeclaration (SingleTypeImportDeclaration i) {
	    TypeName dn = i.getName ();
	    String dotName = dn.getDotName ();
	    FullNameHandler name = FullNameHandler.ofSimpleClassName (dotName);
	    FullNameHandler visibleType = getVisibleType (null, name);
	    if (visibleType == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.position (),
							     "Imported type not found: %s", name));
		i.markUsed (); // Unused, but already flagged as bad, don't want multiple lines
	    }
	    ImportHolder prev = stid.put (dn.getLastPart (), new ImportHolder (i, dotName));
	    if (prev != null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.position (),
							     "Name clash for import: %s", dn.getLastPart ()));
	    }
	}

	private void addSingleStaticImportDeclaration (SingleStaticImportDeclaration i) {
	    String dotName = i.getName ().getDotName ();
	    FullNameHandler name = FullNameHandler.ofSimpleClassName (dotName);
	    FullNameHandler visibleType = getVisibleType (null, name);
	    if (visibleType == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.position (),
							     "Imported type not found: %s", dotName));
		i.markUsed (); // Unused, but already flagged as bad, don't want multiple lines
		return;
	    }

	    ssid.put (i.getInnerId (), new StaticImportType (i));
	}

	private class StaticImportType {
	    private final SingleStaticImportDeclaration id;
	    private final String containingClass;
	    private final String fullName;

	    public StaticImportType (SingleStaticImportDeclaration id) {
		this.id = id;
		containingClass = id.getType ().getDotName ();
		fullName = containingClass + "." + id.getInnerId ();
	    }
	}
    }

    private boolean sameTopLevelClass (FullNameHandler fqn, FullNameHandler topLevelClass) {
	return startsWithOuter (fqn, topLevelClass);
    }

    private static record ImportHolder (ImportDeclaration i, String name) {
	public void markUsed () {
	    if (i != null)
		i.markUsed ();
	}
    }

    private FullNameHandler getVisibleType (EnclosingTypes et, FullNameHandler fqn) {
	FullNameHandler topLevelClass = et == null ? null : lastFQN (et);
	return getVisibleTypeIn (et, fqn, topLevelClass);
    }

    private FullNameHandler lastFQN (EnclosingTypes et) {
	FullNameHandler fqn = null;
	while (et != null) {
	    FullNameHandler etFqn = et.fqn ();
	    if (etFqn != null)
		fqn = etFqn;
	    et = et.previous ();
	}
	return fqn;
    }

    private FullNameHandler getVisibleTypeIn (EnclosingTypes et, FullNameHandler fqn, FullNameHandler topLevelClass) {
	LookupResult r = cip.hasVisibleType (fqn.getFullDotName ());
	if (!r.found ())
	    return null;
	if (!r.found () || !isAccessible (et, fqn, topLevelClass, r))
	    return null;
	return r.fullName ();
    }

    private boolean isAccessible (EnclosingTypes et, FullNameHandler fqn, FullNameHandler topLevelClass, LookupResult r) {
	return isAccessible (et, fqn, topLevelClass, r.accessFlags ());
    }

    private boolean isAccessible (EnclosingTypes et, FullNameHandler fqn, FullNameHandler topLevelClass, int flags) {
	if (Flags.isPublic (flags)) {
	    return true;
	} else if (Flags.isProtected (flags)) {
	    return samePackage (fqn, packageName) || insideSuperClass (et, fqn);
	} else if (Flags.isPrivate (flags)) {
	    return sameTopLevelClass (fqn, topLevelClass);
	}
	// No access level
	if (samePackage (fqn, packageName))
	    return true;
	return false;
    }

    private boolean samePackage (FullNameHandler fnh, String packageName) {
	String fqn = fnh.getFullDotName ();
	String start = packageName == null ? "" : packageName;
	return fqn.length () > start.length () && fqn.startsWith (start) &&
	    fqn.indexOf ('.', fqn.length ()) == -1;
    }

    private boolean insideSuperClass (EnclosingTypes et, FullNameHandler fqn) {
	for (EnclosingTypes c : et)
	    if (c.fqn () != null)
		if (insideSuperClass (fqn, c.fqn ()))
		    return true;
	return false;
    }

    private boolean insideSuperClass (FullNameHandler fqn, FullNameHandler currentClass) {
	try {
	    List<FullNameHandler> supers = cip.getSuperTypes (currentClass.getFullDotName (), false);
	    for (FullNameHandler s :  supers) {
		if (startsWithOuter (fqn, s)) {
		    return true;
		}
		if (insideSuperClass (fqn, s))
		    return true;
	    }
	} catch (IOException e) {
	    return false;
	}
	return false;
    }

    private boolean startsWithOuter (FullNameHandler inner, FullNameHandler outer) {
	String c = inner.getFullDotName ();
	String t = outer.getFullDotName ();
	return c.length () > t.length () && c.startsWith (t) && c.charAt (t.length ()) == '.';
    }

    /** Try to find an outer class that has the inner classes for misdirected outer classes.
     */
    // TODO: We need to verify if this works correctly
    private ResolvedClass tryAllParts (EnclosingTypes et, NamePartHandler nph, ParsePosition pp) {
	StringBuilder sb = new StringBuilder ();
	// group package names to class "java.util.HashMap"
	for (int i = 0, s = nph.size (); i < s; i++) {
	    if (i > 0)
		sb.append (".");
	    String id = nph.getNamePart (i);
	    sb.append (id);
	    ResolvedClass outerClass = resolve (et, FullNameHandler.ofSimpleClassName (sb.toString ()), pp);
	    if (outerClass != null) {
		// Ok, now check if Entry is an inner class either directly or in super class
		FullNameHandler fqn = checkForInnerClasses (et, nph, i + 1, outerClass.type);
		if (fqn != null)
		    return new ResolvedClass (fqn);
	    }
	}
	return null;
    }

    private FullNameHandler checkForInnerClasses (EnclosingTypes et, NamePartHandler nph, int i, FullNameHandler outerClass) {
	FullNameHandler currentOuterClass = outerClass;
	for (int s = nph.size (); i < s; i++) {
	    String id = nph.getNamePart (i);
	    FullNameHandler directInnerClass = currentOuterClass.getInnerClass (id);
	    FullNameHandler visibleType = getVisibleType (et, directInnerClass);
	    if (visibleType != null) {
		currentOuterClass = visibleType;
	    } else {
		currentOuterClass = checkSuperClassesHasInnerClass (et, currentOuterClass, id);
		if (currentOuterClass == null)
		    return null;
	    }
	}
	return currentOuterClass;
    }

    private ResolvedClass resolve (EnclosingTypes et, FullNameHandler name, ParsePosition pos) {
	// Is it a direct class?
	FullNameHandler visibleType = getVisibleType (et, name);
	if (visibleType != null)
	    return new ResolvedClass (visibleType);

	FullNameHandler currentClass = currentClass (et);
	String simpleName = name.getFullDotName ();

	// Check inner classes in the current class
	ResolvedClass r = resolveUsingInnerClass (et, currentClass, name);
	if (r != null)
	    return r;

	// Check if we have a generic type
	TypeParameter tp = getTypeParameter (et, simpleName);
	if (tp != null)
	    return new ResolvedClass (tp);

	// check for class in current package
	FullNameHandler importedClass = resolveUsingImports (et, simpleName, pos);
	if (importedClass != null)
	    return new ResolvedClass (importedClass);

	// check for inner class in super types
	FullNameHandler superInner = checkSuperClassesHasInnerClass (et, currentClass, simpleName);
	if (superInner != null)
	    return new ResolvedClass (superInner);

	// check outer types
	FullNameHandler outerInner = checkOuterClassesHasInnerClass (et, simpleName);
	if (outerInner != null)
	    return new ResolvedClass (outerInner);

	return null;
    }

    private ResolvedClass resolveUsingInnerClass (EnclosingTypes et, FullNameHandler currentClass, FullNameHandler name) {
	FullNameHandler candidate = currentClass.getInnerClass (name.getFullDotName ());
	FullNameHandler visibleType = getVisibleType (et, candidate);
	if (visibleType != null)
	    return new ResolvedClass (visibleType);
	return null;
    }

    private FullNameHandler currentClass (EnclosingTypes et) {
	while (et != null) {
	    if (et.fqn () != null)
		return et.fqn ();
	    et = et.previous ();
	}
	return null;
    }

    private FullNameHandler checkOuterClassesHasInnerClass (EnclosingTypes et, String name) {
	while (et != null) {
	    FullNameHandler fn = et.fqn ();
	    if (fn != null) {
		FullNameHandler candidate = fn.getInnerClass (name);
		FullNameHandler visibleType = getVisibleType (et, candidate);
		if (visibleType != null)
		    return visibleType;
	    }
	    et = et.previous ();
	}
	return null;
    }

    private FullNameHandler checkSuperClassesHasInnerClass (EnclosingTypes et, FullNameHandler fullCtn, String id) {
	if (fullCtn == null)
	    return null;
	List<FullNameHandler> superclasses = getSuperClasses (fullCtn);
	if (superclasses != null) {
	    for (FullNameHandler superclass : superclasses) {
		FullNameHandler candidate = superclass.getInnerClass (id);
		FullNameHandler visibleType = getVisibleType (et, candidate);
		if (visibleType != null)
		    return visibleType;
		FullNameHandler ssn = checkSuperClassesHasInnerClass (et, superclass, id);
		if (ssn != null)
		    return ssn;
	    }
	}
	return null;
    }

    private List<FullNameHandler> getSuperClasses (FullNameHandler type) {
	try {
	    return cip.getSuperTypes (type.getFullDotName (), type.isArray ());
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to load class: " + type, e));
	}
	return Collections.emptyList ();
    }

    private FullNameHandler resolveUsingImports (EnclosingTypes et, String id, ParsePosition pos) {
	ImportHolder i = ih.stid.get (id);
	if (i != null) {
	    FullNameHandler visibleType = getVisibleType (et, FullNameHandler.ofSimpleClassName (i.name ()));
	    if (visibleType != null) {
		i.markUsed ();
		return visibleType;
	    }
	}
	FullNameHandler fqn, visibleType;
	fqn = tryPackagename (id);
	if (fqn != null && (visibleType = getVisibleType (et, fqn)) != null)
	    return visibleType;
	fqn = trySingleStaticImport (id);
	if (fqn != null && (visibleType = getVisibleType (et, fqn)) != null)
	    return visibleType;
	fqn = tryTypeImportOnDemand (et, id, pos);
	if (fqn != null) // already checked cip.hasType
	    return fqn;
	fqn = tryStaticImportOnDemand (et, id);
	if (fqn != null) // already checked cip.hasType
	    return fqn;

	return null;
    }

    private TypeParameter getTypeParameter (EnclosingTypes et, String id) {
	return et.getTypeParameter (id);
    }

    private FullNameHandler tryPackagename (String id) {
	if (packageName == null)
	    return null;
	return FullNameHandler.ofSimpleClassName (packageName + "." + id);
    }

    private FullNameHandler tryTypeImportOnDemand (EnclosingTypes et, String id, ParsePosition pos) {
	List<FullNameHandler> matches = new ArrayList<> ();
	for (ImportHolder ih : ih.tiod) {
	    FullNameHandler visibleType = getVisibleType (et, FullNameHandler.ofSimpleClassName (ih.name () + "." + id));
	    if (visibleType != null) {
		matches.add (visibleType);
		ih.markUsed ();
	    }
	}
	if (matches.size () > 1) {
	    List<String> ls = matches.stream ().map (FullNameHandler::getFullDotName).toList ();
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), pos,
							 "Ambiguous type: %s, possible options: %s", id, ls));
	}
	if (matches.isEmpty ())
	    return null;
	return matches.get (0);
    }

    private FullNameHandler trySingleStaticImport (String id) {
	ImportHandler.StaticImportType sit = ih.ssid.get (id);
	if (sit == null)
	    return null;
	sit.id.markUsed ();
	return FullNameHandler.ofSimpleClassName (sit.fullName);
    }

    private FullNameHandler tryStaticImportOnDemand (EnclosingTypes et, String id) {
	for (StaticImportOnDemandDeclaration siod : ih.siod) {
	    FullNameHandler visibleType =
		getVisibleType (et, FullNameHandler.ofDollarName (siod.getName ().getDotName () + "$" + id));
	    if (visibleType != null) {
		siod.markUsed ();
		return visibleType;
	    }
	}
	return null;
    }

    public void checkUnusedImport () {
	List<ImportDeclaration> imports = ocu.getImports ();
	imports.stream ().filter (i -> !i.hasBeenUsed ()).forEach (i -> checkUnusedImport (i));
    }

    private void checkUnusedImport (ImportDeclaration i) {
	if (i instanceof SingleStaticImportDeclaration) {
	    SingleStaticImportDeclaration si = (SingleStaticImportDeclaration)i;
	    String full = si.getFullName ();
	    FullNameHandler fullName = FullNameHandler.ofSimpleClassName (full);
	    String fqn = si.getType ().getDotName ();
	    String field = si.getInnerId ();
	    FullNameHandler visibleType = getVisibleType (null, fullName);
	    if (visibleType == null && cip.getFieldInformation (visibleType, field) == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.position (),
							     "Type %s has no symbol: %s", fqn, field));
		return;
	    }
	}
	diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
						       i.position (),
						       "Unused import: %s", i.getValue ()));
    }

    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.position (), template, args));
    }

    private String dotName (FullNameHandler fn) {
	if (fn == null)
	    return "-";
	return fn.getFullDotName ();
    }
}
