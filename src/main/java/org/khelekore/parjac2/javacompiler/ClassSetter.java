package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.AmbiguousName;
import org.khelekore.parjac2.javacompiler.syntaxtree.Annotation;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.BlockStatements;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassOrInterfaceTypeToInstantiate;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.DottedName;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumConstant;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExceptionTypeList;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExpressionName;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterList;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.ImportDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.LocalVariableDeclaration;
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
import org.khelekore.parjac2.javacompiler.syntaxtree.SimpleClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.SingleStaticImportDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.SingleTypeImportDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.StaticImportOnDemandDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.Throws;
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
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ClassSetter {

    private final ClassInformationProvider cip;
    private final ParsedEntry tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final String packageName;          // package name of the file we are looking at
    private final OrdinaryCompilationUnit ocu; // we do not care about ModularCompilationUnit (for now?)

    private final ImportHandler ih;            // keep track of the imports we have

    /** High level description:
     *  For each class:
     *      Setup a Scope
     *      Find fields, set type on them, add to scope of class
     *      Find methods, set type on arguments and add to scope of method
     *      Set type of expression
     */
    public static void fillInClasses (ClassInformationProvider cip,
				      List<ParsedEntry> trees,
				      CompilerDiagnosticCollector diagnostics) {
	List<ClassSetter> classSetters =
	    trees.stream ()
	    .filter (pe -> (pe.getRoot () instanceof OrdinaryCompilationUnit))
	    .map (t -> new ClassSetter (cip, t, diagnostics)).collect (Collectors.toList ());
	if (diagnostics.hasError ())
	    return;
	classSetters.parallelStream ().forEach (ClassSetter::registerSuperTypes);

	classSetters.parallelStream ().forEach (ClassSetter::registerFields);
	classSetters.parallelStream ().forEach (ClassSetter::registerMethods);

	// now that we know what types, fields and methods we have we check the method contents
	classSetters.parallelStream ().forEach (ClassSetter::checkMethodBodies);

	classSetters.parallelStream ().forEach (cs -> cs.checkUnusedImport ());
    }

    public ClassSetter (ClassInformationProvider cip,
			ParsedEntry pe,
			CompilerDiagnosticCollector diagnostics) {
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
	return enclosingTypeParameter (et, nameToTypeParameter);
    }

    private void registerFields () {
	forAllTypes (this::setFieldTypes);
    }

    private void setFieldTypes (TypeDeclaration td, EnclosingTypes et) {
	EnclosingTypes ett = registerTypeParameters (et, td.getTypeParameters ());
	Map<String, FieldInfo> fields = td.getFields ();
	fields.forEach ((name, info) ->  setFieldType (ett, name, info));
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
    }

    private void setMethodTypes (EnclosingTypes bt, MethodDeclarationBase md) {
	checkAnnotations (bt, md.getAnnotations ());
	EnclosingTypes et = registerTypeParameters (bt, md.getTypeParameters ());
	setType (et, md.getResult ());
	ReceiverParameter rp = md.getReceiverParameter ();
	if (rp != null) {
	    checkAnnotations (et, rp.getAnnotations ());
	    setType (et, rp.getType ());
	}
	EnclosingTypes rt = setFormalParameterListTypes (et, md.getFormalParameterList ());

	Throws t = md.getThrows ();
	if (t != null) {
	    ExceptionTypeList exceptions = t.getExceptions ();
	    setTypes (rt, exceptions.get ());
	}
    }

    private void setConstructorTypes (EnclosingTypes bt, ConstructorDeclarationBase cdb) {
	checkAnnotations (bt, cdb.getAnnotations ());
	EnclosingTypes et = registerTypeParameters (bt, cdb.getTypeParameters ());
	setFormalParameterListTypes (et, cdb.getFormalParameterList ());
    }

    private EnclosingTypes setFormalParameterListTypes (EnclosingTypes et, FormalParameterList args) {
	Map<String, VariableInfo> variables = new HashMap<> ();
	if (args != null) {
	    for (FormalParameterBase fp : args.getParameters ()) {
		checkFormalParameterModifiers (et, fp.getModifiers ());
		setType (et, fp.type ());
		String id = fp.name ();
		VariableInfo previous = variables.put (id, fp);
		if (previous != null) {
		    error (fp, "ClassSetter.setFormalParameterListTypes: name %s already in use", id);
		}
	    }
	}
	return enclosingVariables (et, variables);
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

    // TODO: qwerty: remove copy paste from registerMethods/setMethodTypes
    private void checkMethodBodies (TypeDeclaration td, EnclosingTypes et) {
	EnclosingTypes ett = registerTypeParameters (et, td.getTypeParameters ());
	td.getMethods ().forEach (m -> checkMethodBodies (ett, m));

	td.getConstructors ().forEach (c -> checkConstructorBodies (ett, c));
	td.getInstanceInitializers ().forEach (c -> checkInstanceInitializerBodies (ett, c));
	td.getStaticInitializers ().forEach (c -> checkStaticInitializerBodies (ett, c));
    }

    private void checkMethodBodies (EnclosingTypes et, MethodDeclarationBase md) {
	EnclosingTypes tt = registerTypeParameters (et, md.getTypeParameters ());
	EnclosingTypes rt = setFormalParameterListTypes (tt, md.getFormalParameterList ());
	ParseTreeNode body = md.getMethodBody ();
	if (body instanceof Block block) {
	    EnclosingTypes ms = enclosingBlock (rt);
	    BlockStatements bs = block.getStatements ();
	    if (bs != null) {
		List<ParseTreeNode> statements = bs.getStatements ();
		statements.forEach (s -> setTypesForMethodStatement (ms, List.of (s)));
	    }
	}
    }

    private void checkConstructorBodies (EnclosingTypes et, ConstructorDeclarationBase cdb) {
	et = registerTypeParameters (et, cdb.getTypeParameters ());
	et = setFormalParameterListTypes (et, cdb.getFormalParameterList ());
	et = enclosingBlock (et);
	setTypesForMethodStatement (et, cdb.getStatements ());
    }

    private void checkInstanceInitializerBodies (EnclosingTypes et, ParseTreeNode p) {
	et = enclosingBlock (et);
	setTypesForMethodStatement (et, List.of (p));
    }

    private void checkStaticInitializerBodies (EnclosingTypes et, ParseTreeNode p) {
	et = enclosingBlock (et);
	setTypesForMethodStatement (et, List.of (p));
    }

    private void setTypesForMethodStatement (EnclosingTypes et, List<ParseTreeNode> ps) {
	Deque<Object> partsToHandle = new ArrayDeque<> ();
	partsToHandle.addAll (ps);
	while (!partsToHandle.isEmpty ()) {
	    Object pp = partsToHandle.removeFirst ();
	    switch (pp) {
	    case ClassType ct -> setType (et, ct);
	    case AmbiguousName an -> reclassifyAmbiguousName (et, an);
	    case ExpressionName en -> reclassifyAmbiguousName (et, en);
	    case DottedName dt -> setType (et, dt);
	    case MethodReference mr -> {
		// skip for now
	    }
	    case ClassOrInterfaceTypeToInstantiate coitti -> setType (et, coitti.getType ());

	    // Since we add first we will evaluate variable addition after the parts, which is what we want.
	    case LocalVariableDeclaration lv -> { partsToHandle.add (new AddVariable (lv)); addParts (partsToHandle, lv); }

	    // Check method once we know all parts
	    case MethodInvocation mi -> { partsToHandle.add (new MethodInvocationCheck (mi, this)); addParts (partsToHandle, mi); }

	    case ParseTreeNode ptn -> addParts (partsToHandle, ptn);

	    case CustomHandler ch -> ch.run (et);

	    default -> throw new IllegalStateException ("Unhandled type: " + pp);
	    }
	}
    }

    private void reclassifyAmbiguousName (EnclosingTypes et, DottedName an) {
	reclassifyAndSetTypeOnAmbiguousName (et, an);
	if (an.getFullNameHandler () == null)
	    error (an, "Unable to find symbol: %s", an.getDotName ());
    }

    private void reclassifyAndSetTypeOnAmbiguousName (EnclosingTypes et, DottedName an) {
	if (an.size () == 1) {
	    tryToSetFullNameOnSimpleName (et, an);
	} else {
	    DottedName leftPart = an.allButLast ();
	    reclassifyAndSetTypeOnAmbiguousName (et, leftPart);
	    FullNameHandler fn = leftPart.getFullNameHandler ();
	    if (fn == null) { // (part of) package name
		// since name is fully specified we do not want to check outer enclosures
		FullNameHandler fqn = getVisibleType (null, FullNameHandler.ofSimpleClassName (an.getDotName ()));
		if (fqn != null)
		    an.setFullName (fqn);
	    } else { // we have a type
		String id = an.getLastPart ();
		VariableInfo fi = cip.getFieldInformation (fn, id);
		// if we find a field we can not access we can not log an error since doing so will
		// result in us reporting multiple errors. We have to rely on the "unable to find symbol" from above.
		if (fi != null && isAccessible (et, fn, fi)) {
		    an.setFullName (fi.typeName ());
		    // TODO: probably need to handle ExpressionName as well
		    an.replace (new FieldAccess (an.position (), leftPart, id));
		} else {
		    FullNameHandler innerClassCandidate = fn.getInnerClass (id);
		    FullNameHandler foundInnerClass = getVisibleType (null, innerClassCandidate);
		    if (foundInnerClass != null)
			an.setFullName (foundInnerClass);
		}
	    }
	}
    }

    private FullNameHandler tryToSetFullNameOnSimpleName (EnclosingTypes et, DottedName an) {
	String name = an.getLastPart ();
	VariableInfo fi = getVariable (et, name);
	if (fi != null) { // known variable
	    FieldAccess access = new FieldAccess (an.position (), null, name);
	    an.replace (access);
	    an.setFullName (FullNameHandler.type (fi.type ()));
	} else {
	    FullNameHandler fn = FullNameHandler.ofSimpleClassName (name);
	    ResolvedClass fqn = resolve (et, fn, an.position ());
	    if (fqn != null) {
		an.setFullName (fqn.type);
		an.replace (new ClassType (fqn.type));
	    }
	}
	return an.getFullNameHandler ();
    }

    private VariableInfo getVariable (EnclosingTypes et, String name) {
	while (et != null) {
	    VariableInfo fi = et.enclosure ().getField (name);
	    if (fi != null)
		return fi;
	    et = et.previous ();
	}
	return null;
    }

    private void addParts (Deque<Object> partsToHandle, ParseTreeNode pp) {
	List<ParseTreeNode> parts = pp.getChildren ();
	for (int i = parts.size () - 1; i >= 0; i--)
	    partsToHandle.addFirst (parts.get (i));
    }

    private static record AddVariable (LocalVariableDeclaration lv) implements CustomHandler {
	@Override public void run (EnclosingTypes et) {
	    BlockEnclosure be = (BlockEnclosure)et.enclosure ();
	    be.add (lv);
	}
    }

    private record MethodInvocationCheck (MethodInvocation mi, ClassSetter cs) implements CustomHandler {
	@Override public void run (EnclosingTypes et) {
	    FullNameHandler currentClass = cs.currentClass (et);
	    cs.checkMethodCall (currentClass, mi);
	}
    }

    private interface CustomHandler {
	void run (EnclosingTypes et);
    }

    private void checkMethodCall (FullNameHandler nameOfCurrentClass, MethodInvocation mi) {
	ParseTreeNode on = mi.getOn ();
	boolean isSuper = mi.isSuper ();
	String name = mi.getMethodName ();
	List<ParseTreeNode> args = mi.getArguments ();

	FullNameHandler methodOn = nameOfCurrentClass;
	if (on != null) {
	    // TODO: set methodOn
	    methodOn = FullNameHandler.type (on);
	}
	if (isSuper) {
	    // TODO: set methodOn
	}

	// AmbiguousName that could not be resolved and similar, no need for NPE or another error
	if (methodOn == null)
	    return;

	// TODO: verify method arguments match
	List<MethodInfo> options = cip.getMethods (methodOn, name);
	if (options == null) {
	    error (mi, "No method named %s found in %s", name, methodOn);
	    return;
	}
	for (MethodInfo info : options) {
	    if (match (on, args, info)) {
		mi.result (info.result ());
	    }
	}
	if (mi.result () == null)
	    error (mi, "No matching method found");
    }

    private boolean match (ParseTreeNode on, List<ParseTreeNode> args, MethodInfo info) {
	if (on instanceof DottedName dn)
	    on = dn.replaced ();
	boolean requireInstance = !Flags.isStatic (info.flags ());
	boolean varArgs = Flags.isVarArgs (info.flags ());
	if (!varArgs && args.size () != info.numberOfArguments ())
	    return false;

	if (requireInstance && on instanceof ClassType)
	    return false;

	// TODO: check types
	return true;
    }

    private boolean isAccessible (EnclosingTypes et, FullNameHandler fqn, VariableInfo fi) {
	FullNameHandler topLevelClass = et == null ? null : lastFQN (et);
	return isAccessible (et, fqn, topLevelClass, fi.flags ());
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
	ocu.getTypes ().stream ().map (td -> enclosingTypes (null, td)).forEach (typesToHandle::add);
	while (!typesToHandle.isEmpty ()) {
	    EnclosingTypes et = typesToHandle.removeFirst ();
	    TypeDeclaration td = et.td ();
	    handler.accept (td, et);
	    td.getInnerClasses ().stream ().map (i -> enclosingTypes (et, i)).forEach (typesToHandle::add);
	}
    }

    private void setType (EnclosingTypes et, ParseTreeNode type) {
	switch (type) {
	case TokenNode t -> { /* empty */ }
	case PrimitiveType p -> { /* empty */ }
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
	ct.setFullName (fqn.type);
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
	tn.setFullName (fqn.type);
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
	    Optional<List<FullNameHandler>> supers = cip.getSuperTypes (currentClass.getFullDotName (), false);
	    if (supers.isPresent ()) {
		for (FullNameHandler s :  supers.get ()) {
		    if (startsWithOuter (fqn, s)) {
			return true;
		    }
		    if (insideSuperClass (fqn, s))
			return true;
		}
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


    private static class ResolvedClass {
	public final FullNameHandler type;
	public final TypeParameter tp;

	public ResolvedClass (FullNameHandler type) {
	    this.type = type;
	    this.tp = null;
	}

	public ResolvedClass (TypeParameter tp) {
	    this.type = tp.getExpressionType ().getFullNameHandler ();
	    this.tp = tp;
	}

	@Override public String toString () {
	    return "ResolvedClass{type: " + type.getFullDollarName () + ", tp: " + tp + "}";
	}
    }

    private FullNameHandler checkSuperClassesHasInnerClass (EnclosingTypes et, FullNameHandler fullCtn, String id) {
	if (fullCtn == null)
	    return null;
	List<FullNameHandler> superclasses = getSuperClasses (fullCtn);
	for (FullNameHandler superclass : superclasses) {
	    FullNameHandler candidate = superclass.getInnerClass (id);
	    FullNameHandler visibleType = getVisibleType (et, candidate);
	    if (visibleType != null)
		return visibleType;
	    FullNameHandler ssn = checkSuperClassesHasInnerClass (et, superclass, id);
	    if (ssn != null)
		return ssn;
	}
	return null;
    }

    private List<FullNameHandler> getSuperClasses (FullNameHandler type) {
	try {
	    Optional<List<FullNameHandler>> supers = cip.getSuperTypes (type.getFullDotName (), false);
	    return supers.isPresent () ? supers.get () : Collections.emptyList ();
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

    private EnclosingTypes enclosingTypes (EnclosingTypes previous, TypeDeclaration td) {
	return new EnclosingTypes (previous, new TypeEnclosure (td, cip.getFullName (td)));
    }

    private static EnclosingTypes enclosingTypeParameter (EnclosingTypes previous, Map<String, TypeParameter> nameToTypeParameter) {
	return new EnclosingTypes (previous, new TypeParameterEnclosure (nameToTypeParameter));
    }

    private static EnclosingTypes enclosingVariables (EnclosingTypes previous, Map<String, VariableInfo> nameToVariable) {
	return new EnclosingTypes (previous, new VariableEnclosure (nameToVariable));
    }

    private static EnclosingTypes enclosingBlock (EnclosingTypes previous) {
	return new EnclosingTypes (previous, new BlockEnclosure ());
    }

    private record EnclosingTypes (EnclosingTypes previous, Enclosure<?> enclosure)
	implements Iterable<EnclosingTypes> {

	@Override public Iterator<EnclosingTypes> iterator () {
	    return new ETIterator (this);
	}

	public TypeDeclaration td () {
	    return enclosure.td ();
	}

	public FullNameHandler fqn () {
	    return enclosure.fqn ();
	}

	public TypeParameter getTypeParameter (String id) {
	    return enclosure.getTypeParameter (id);
	}

	private static class ETIterator implements Iterator<EnclosingTypes> {
	    private EnclosingTypes e;

	    public ETIterator (EnclosingTypes e) {
		this.e = e;
	    }

	    @Override public boolean hasNext () {
		return e != null;
	    }

	    @Override public EnclosingTypes next () {
		EnclosingTypes ret = e;
		e = e.previous ();
		return ret;
	    }
	}
    }

    private interface Enclosure<V extends VariableInfo> {
	default TypeDeclaration td () { return null; }
	default FullNameHandler fqn () { return null; }
	default TypeParameter getTypeParameter (String id) { return null; }
	Map<String, V> getFields ();
	default V getField (String name) { return getFields ().get (name); }
    }

    private record TypeEnclosure (TypeDeclaration td, FullNameHandler fqn) implements Enclosure<FieldInfo> {
	@Override public TypeDeclaration td () { return td; }
	@Override public FullNameHandler fqn () { return fqn; }
	@Override public Map<String, FieldInfo> getFields () { return td.getFields (); }
    }

    private record TypeParameterEnclosure (Map<String, TypeParameter> nameToTypeParameter) implements Enclosure<VariableInfo> {
	@Override public TypeParameter getTypeParameter (String id) { return nameToTypeParameter.get (id); }
	@Override public Map<String, VariableInfo> getFields () { return Map.of (); }
    }

    private record VariableEnclosure (Map<String, VariableInfo> variables) implements Enclosure<VariableInfo> {
	@Override public Map<String, VariableInfo> getFields () { return variables; }
    }

    private static class BlockEnclosure implements Enclosure<VariableInfo> {
	private Map<String, VariableInfo> locals = Map.of ();

	private void add (LocalVariableDeclaration lv) {
	    if (locals.isEmpty ())
		locals = new HashMap<> ();
	    for (VariableDeclarator vd : lv.getDeclarators ()) {
		String name = vd.getName ();
		VariableInfo vi = new FieldInfo (name, vd.position (), Flags.ACC_PUBLIC, lv.getType (), vd.rank ());
		locals.put (name, vi);
	    }
	}

	@Override public Map<String, VariableInfo> getFields () {
	    return locals;
	}
    }

    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.position (), template, args));
    }
}
