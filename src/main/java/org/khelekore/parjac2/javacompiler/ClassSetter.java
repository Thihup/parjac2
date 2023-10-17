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
import org.khelekore.parjac2.javacompiler.syntaxtree.Annotation;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumConstant;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExceptionTypeList;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterList;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.ImportDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclarationBase;
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

    /* qwerty: TODO: remove
    private final Map<ParseTreeNode, TypeHolder> types = new HashMap<> ();
    private TypeHolder currentTypes = null;
    */

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

    public void registerSuperTypes () {
	forAllTypes ((td, et) -> {
		switch (td) {
		case NormalClassDeclaration ndi -> registerSuperTypes (et, ndi);
		case EnumDeclaration ed -> setTypes (et, ed.getSuperInterfaces ());
		case NormalInterfaceDeclaration i -> registerSuperTypes (et, i);
		case RecordDeclaration rd -> setTypes (et, rd.getSuperInterfaces ());
		case UnqualifiedClassInstanceCreationExpression uc -> setType (et, uc.getSuperType ());
		case EnumConstant ec -> setTypes (et, ec.getParent ().getSuperInterfaces ());
		default -> error (td, "ClassSetter.registerSuperTypes: Unhandled type: %s", td.getClass ().getName ());
		}
	    });
    }

    private void registerSuperTypes (EnclosingTypes et, NormalClassDeclaration ndi) {
	ClassType superclass = ndi.getSuperClass ();
	if (superclass != null)
	    setType (et, superclass);
	setTypes (et, ndi.getSuperInterfaces ());
	registerTypeParameters (et, ndi.getTypeParameters ());
    }

    private void registerSuperTypes (EnclosingTypes et, NormalInterfaceDeclaration i) {
	setTypes (et, i.getExtendsInterfaces ());
    }

    private void registerTypeParameters (EnclosingTypes et, TypeParameters tps) {
	if (tps == null)
	    return;
	for (TypeParameter tp : tps.get ()) {
	    // TODO: register the type parameter tp.getId ();
	    TypeBound bound = tp.getTypeBound ();
	    if (bound != null) {
		setType (et, bound.getType ());
		bound.getAdditionalBounds ().forEach (b -> setType (et, b));
	    }
	}
    }

    private void registerFields () {
	forAllTypes (this::setFieldTypes);
    }

    private void setFieldTypes (TypeDeclaration td, EnclosingTypes et) {
	Map<String, FieldInfo> fields = td.getFields ();
	fields.forEach ((name, info) ->  setFieldType (et, name, info));
    }

    private void setFieldType (EnclosingTypes et, String name, FieldInfo info) {
	setType (et, info.fd ().getType ());
    }

    private void registerMethods () {
	forAllTypes (this::setMethodTypes);
    }

    private void setMethodTypes (TypeDeclaration td, EnclosingTypes et) {
	List<? extends MethodDeclarationBase> methods = td.getMethods ();
	methods.forEach (m -> setMethodTypes (et, m));

	// TODO: handle constructors
	// TODO: handle instance and static blocks
    }

    private void setMethodTypes (EnclosingTypes et, MethodDeclarationBase md) {
	registerTypeParameters (et, md.getTypeParameters ());
	checkAnnotations (et, md.getAnnotations ());
	setType (et, md.getResult ());
	ReceiverParameter rp = md.getReceiverParameter ();
	if (rp != null) {
	    checkAnnotations (et, rp.getAnnotations ());
	    setType (et, rp.getType ());
	}
	FormalParameterList args = md.getFormalParameterList ();
	if (args != null) {
	    for (FormalParameterBase fp : args.getParameters ()) {
		checkFormalParameterModifiers (et, fp.getModifiers ());
		setType (et, fp.getType ());
	    }
	}

	Throws t = md.getThrows ();
	if (t != null) {
	    ExceptionTypeList exceptions = t.getExceptions ();
	    setTypes (et, exceptions.get ());
	}
	ParseTreeNode body = md.getMethodBody ();
	// TODO: handle body
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

    private void checkFormalParameterModifiers (EnclosingTypes et, List<ParseTreeNode> modifiers) {
	for (ParseTreeNode n : modifiers) {
	    if (n instanceof Annotation a)
		setType (et, a.getTypeName ());
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
	/* Not sure if we need this
	if (type instanceof PrimitiveTokenType) {
	    return;
	}
	*/
	switch (type) {
	case TokenNode t -> {}
	case PrimitiveType p -> {}
	case ClassType ct -> setType (et, ct);
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
	FullNameHandler id = getDottedName (ct);
	ResolvedClass fqn = resolve (et, id, ct.getPosition ());
	if (fqn == null && ct.size () > 1) {
	    // ok, someone probably wrote something like "HashMap.Entry" or
	    // "java.util.HashMap.Entry" which is somewhat problematic, but legal
	    fqn = tryAllParts (et, ct);
	}
	if (fqn == null) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.getPosition (),
							 "Failed to find class: %s", id));
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

    private FullNameHandler getDottedName (ClassType ct) {
	String dotName;
	if (ct.size () == 1)
	    dotName = ct.get ().get (0).getId ();
	else
	    dotName = ct.get ().stream ().map (s -> s.getId ()).collect (Collectors.joining ("."));
	return FullNameHandler.ofSimpleClassName (dotName);
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

	private void addSingleTypeImportDeclaration (SingleTypeImportDeclaration i) {
	    TypeName dn = i.getName ();
	    String dotName = dn.getDotName ();
	    FullNameHandler name = FullNameHandler.ofSimpleClassName (dotName);
	    FullNameHandler visibleType = getVisibleType (null, name);
	    if (visibleType == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getPosition (),
							     "Imported type not found: %s", name));
		i.markUsed (); // Unused, but already flagged as bad, don't want multiple lines
	    }
	    ImportHolder prev = stid.put (dn.getLastPart (), new ImportHolder (i, dotName));
	    if (prev != null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getPosition (),
							     "Name clash for import: %s", dn.getLastPart ()));
	    }
	}

	private void addSingleStaticImportDeclaration (SingleStaticImportDeclaration i) {
	    String dotName = i.getName ().getDotName ();
	    FullNameHandler name = FullNameHandler.ofSimpleClassName (dotName);
	    FullNameHandler visibleType = getVisibleType (null, name);
	    if (visibleType == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getPosition (),
							     "Imported type not found: %s", dotName));
		i.markUsed (); // Unused, but already flagged as bad, don't want multiple lines
		return;
	    }

	    ssid.put (i.getInnerId (), new StaticImportType (i));
	}

	private class StaticImportType {
	    private final SingleStaticImportDeclaration ssid;
	    private final String containingClass;
	    private final String fullName;

	    public StaticImportType (SingleStaticImportDeclaration ssid) {
		this.ssid = ssid;
		containingClass = ssid.getType ().getDotName ();
		fullName = containingClass + "." + ssid.getInnerId ();
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
	    fqn = et.fqn ();
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
	if (Flags.isPublic (r.accessFlags ())) {
	    return true;
	} else if (Flags.isProtected (r.accessFlags ())) {
	    return samePackage (fqn, packageName) || insideSuperClass (et, fqn);
	} else if (Flags.isPrivate (r.accessFlags ())) {
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
    private ResolvedClass tryAllParts (EnclosingTypes et, ClassType ct) {
	StringBuilder sb = new StringBuilder ();
	List<SimpleClassType> scts = ct.get ();
	// group package names to class "java.util.HashMap"
	for (int i = 0, s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    if (i > 0)
		sb.append (".");
	    sb.append (sct.getId ());
	    ResolvedClass outerClass = resolve (et, FullNameHandler.ofSimpleClassName (sb.toString ()), ct.getPosition ());
	    if (outerClass != null) {
		// Ok, now check if Entry is an inner class either directly or in super class
		FullNameHandler fqn = checkForInnerClasses (et, scts, i + 1, outerClass.type);
		if (fqn != null)
		    return new ResolvedClass (fqn);
	    }
	}
	return null;
    }

    private FullNameHandler checkForInnerClasses (EnclosingTypes et, List<SimpleClassType> scts, int i, FullNameHandler outerClass) {
	FullNameHandler currentOuterClass = outerClass;
	for (int s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    FullNameHandler directInnerClass = currentOuterClass.getInnerClass (sct.getId ());
	    FullNameHandler visibleType = getVisibleType (et, directInnerClass);
	    if (visibleType != null) {
		currentOuterClass = visibleType;
	    } else {
		currentOuterClass = checkSuperClasses (et, currentOuterClass, sct.getId ());
		if (currentOuterClass == null)
		    return null;
	    }
	}
	return currentOuterClass;
    }

    private ResolvedClass resolve (EnclosingTypes et, FullNameHandler name, ParsePosition pos) {
	String simpleName = name.getFullDotName ();
	TypeParameter tp = getTypeParameter (simpleName);
	if (tp != null) {
	    return new ResolvedClass (tp);
	}

	FullNameHandler visibleType = getVisibleType (et, name);
	if (visibleType != null)
	    return new ResolvedClass (visibleType);

	FullNameHandler fqn = resolveInnerClass (et, simpleName);
	if (fqn == null)
	    fqn = resolveUsingImports (et, simpleName, pos);
	if (fqn != null)
	    return new ResolvedClass (fqn);
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
	    return "ResolvedClass{type: " + type + ", tp: " + tp + "}";
	}
    }

    private FullNameHandler resolveInnerClass (EnclosingTypes et, String id) {
	// Check for inner class
	for (EnclosingTypes ctn : et) {
	    FullNameHandler visibleType = getVisibleType (et, ctn.fqn ().getInnerClass (id));
	    if (visibleType != null)
		return visibleType;
	}

	// Check for inner class of super classes
	for (EnclosingTypes ctn : et) {
	    FullNameHandler fqn = checkSuperClasses (et, ctn.fqn (), id);
	    if (fqn != null)
		return fqn;
	}
	return null;
    }

    private FullNameHandler checkSuperClasses (EnclosingTypes et, FullNameHandler fullCtn, String id) {
	if (fullCtn == null)
	    return null;
	List<FullNameHandler> superclasses = getSuperClasses (fullCtn);
	for (FullNameHandler superclass : superclasses) {
	    FullNameHandler visibleType = getVisibleType (et, superclass.getInnerClass (id));
	    if (visibleType != null)
		return visibleType;
	    FullNameHandler ssn = checkSuperClasses (et, superclass, id);
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
	fqn = tryPackagename (id, pos);
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

    private TypeParameter getTypeParameter (String id) {
	/* TODO: implement correctly
	TypeHolder th = currentTypes;
	while (th != null) {
	    TypeParameter tp = th.types.get (id);
	    if (tp != null)
		return tp;
	    th = th.parent;
	}
	*/
	return null;
    }

    private FullNameHandler tryPackagename (String id, ParsePosition pos) {
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
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), pos,
							 "Ambigous type: %s: %s", id, matches));
	}
	if (matches.isEmpty ())
	    return null;
	return matches.get (0);
    }

    private FullNameHandler trySingleStaticImport (String id) {
	ImportHandler.StaticImportType sit = ih.ssid.get (id);
	if (sit == null)
	    return null;
	sit.ssid.markUsed ();
	return FullNameHandler.ofSimpleClassName (sit.fullName);
    }

    private FullNameHandler tryStaticImportOnDemand (EnclosingTypes et, String id) {
	for (StaticImportOnDemandDeclaration siod : ih.siod) {
	    FullNameHandler visibleType = getVisibleType (et, FullNameHandler.ofDollarNAme (siod.getName ().getDotName () + "$" + id));
	    if (visibleType != null) {
		siod.markUsed ();
		return visibleType;
	    }
	}
	return null;
    }

    /** qwerty TODO remove this */
    /*
    private static class TypeHolder {
	private final TypeHolder parent;
	private final Map<String, TypeParameter> types;

	public TypeHolder (TypeHolder parent, Map<String, TypeParameter> types) {
	    this.parent = parent;
	    this.types = types;
	}
    }
    */

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
	    if (visibleType == null && cip.getFieldInformation (fqn, field) == null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getPosition (),
							     "Type %s has no symbol: %s", fqn, field));
		return;
	    }
	}
	diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (),
						       i.getPosition (),
						       "Unused import: %s", i.getValue ()));
    }

    public EnclosingTypes enclosingTypes (EnclosingTypes previous, TypeDeclaration td) {
	return new EnclosingTypes (previous, td, cip.getFullName (td));
    }

    private record EnclosingTypes (EnclosingTypes previous, TypeDeclaration td, FullNameHandler fqn)
	implements Iterable<EnclosingTypes> {

	public Iterator<EnclosingTypes> iterator () {
	    return new ETIterator (this);
	}

	private static class ETIterator implements Iterator<EnclosingTypes> {
	    private EnclosingTypes e;

	    public ETIterator (EnclosingTypes e) {
		this.e = e;
	    }

	    public boolean hasNext () {
		return e != null;
	    }

	    public EnclosingTypes next () {
		EnclosingTypes ret = e;
		e = e.previous ();
		return ret;
	    }
	}
    }

    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.getPosition (), template, args));
    }
}
