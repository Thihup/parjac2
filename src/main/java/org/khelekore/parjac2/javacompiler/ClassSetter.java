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
import java.util.function.Consumer;
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

    // The list of outer classes, currently changed while we loop over inner classes
    private EnclosingTypes containingTypes = null;

    private final Map<ParseTreeNode, TypeHolder> types = new HashMap<> ();
    private TypeHolder currentTypes = null;

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
	forAllTypes (td -> {
		switch (td) {
		case NormalClassDeclaration ndi -> registerSuperTypes (ndi);
		case EnumDeclaration ed -> setTypes (ed.getSuperInterfaces ());
		case NormalInterfaceDeclaration i -> registerSuperTypes (i);
		case RecordDeclaration rd -> setTypes (rd.getSuperInterfaces ());
		case UnqualifiedClassInstanceCreationExpression uc -> setType (uc.getSuperType ());
		case EnumConstant ec -> setTypes (ec.getParent ().getSuperInterfaces ());
		default -> error (td, "ClassSetter.registerSuperTypes: Unhandled type: %s", td.getClass ().getName ());
		}
	    });
    }

    private void registerSuperTypes (NormalClassDeclaration ndi) {
	ClassType superclass = ndi.getSuperClass ();
	if (superclass != null)
	    setType (superclass);
	setTypes (ndi.getSuperInterfaces ());
	registerTypeParameters (ndi.getTypeParameters ());
    }

    private void registerSuperTypes (NormalInterfaceDeclaration i) {
	setTypes (i.getExtendsInterfaces ());
    }

    private void registerTypeParameters (TypeParameters tps) {
	if (tps == null)
	    return;
	for (TypeParameter tp : tps.get ()) {
	    // TODO: register the type parameter tp.getId ();
	    TypeBound bound = tp.getTypeBound ();
	    if (bound != null) {
		setType (bound.getType ());
		bound.getAdditionalBounds ().forEach (this::setType);
	    }
	}
    }

    private void registerFields () {
	forAllTypes (this::setFieldTypes);
    }

    private void setFieldTypes (TypeDeclaration td) {
	Map<String, FieldInfo> fields = td.getFields ();
	fields.forEach (this::setFieldType);
    }

    private void setFieldType (String name, FieldInfo info) {
	setType (info.fd ().getType ());
    }

    private void registerMethods () {
	forAllTypes (this::setMethodTypes);
    }

    private void setMethodTypes (TypeDeclaration td) {
	List<? extends MethodDeclarationBase> methods = td.getMethods ();
	methods.forEach (this::setMethodTypes);

	// TODO: handle constructors
	// TODO: handle instance and static blocks
    }

    private void setMethodTypes (MethodDeclarationBase md) {
	registerTypeParameters (md.getTypeParameters ());
	checkAnnotations (md.getAnnotations ());
	setType (md.getResult ());
	ReceiverParameter rp = md.getReceiverParameter ();
	if (rp != null) {
	    checkAnnotations (rp.getAnnotations ());
	    setType (rp.getType ());
	}
	FormalParameterList args = md.getFormalParameterList ();
	if (args != null) {
	    for (FormalParameterBase fp : args.getParameters ()) {
		checkFormalParameterModifiers (fp.getModifiers ());
		setType (fp.getType ());
	    }
	}

	Throws t = md.getThrows ();
	if (t != null) {
	    ExceptionTypeList exceptions = t.getExceptions ();
	    setTypes (exceptions.get ());
	}
	ParseTreeNode body = md.getMethodBody ();
	// TODO: handle body
    }

    private void checkAnnotations (List<? extends ParseTreeNode> annotations) {
	if (annotations == null)
	    return;
	for (ParseTreeNode t : annotations) {
	    switch (t) {
	    case Annotation a -> setType (a.getTypeName ());
	    default -> error (t, "ClassSetter: Unhandled annotation: %s, %s", t.getClass ().getName (), t);
	    }
	}
    }

    private void checkFormalParameterModifiers (List<ParseTreeNode> modifiers) {
	for (ParseTreeNode n : modifiers) {
	    if (n instanceof Annotation a)
		setType (a.getTypeName ());
	}
    }

    private void forAllTypes (Consumer<TypeDeclaration> handler) {
	Deque<EnclosingTypes> typesToHandle = new ArrayDeque<> ();
	ocu.getTypes ().stream ().map (td -> enclosingTypes (null, td)).forEach (typesToHandle::add);
	while (!typesToHandle.isEmpty ()) {
	    EnclosingTypes et = typesToHandle.removeFirst ();
	    containingTypes = et;
	    TypeDeclaration td = et.td ();
	    handler.accept (td);
	    td.getInnerClasses ().stream ().map (i -> enclosingTypes (et, i)).forEach (typesToHandle::add);
	}
    }

    private void setType (ParseTreeNode type) {
	/* Not sure if we need this
	if (type instanceof PrimitiveTokenType) {
	    return;
	}
	*/
	switch (type) {
	case TokenNode t -> {}
	case PrimitiveType p -> {}
	case ClassType ct -> setType (ct);
	case ArrayType at -> setType (at.getType ());
	case Wildcard wc -> {
	    WildcardBounds wb = wc.getBounds ();
	    if (wb != null)
		setType (wb.getClassType ());
	}
	default -> error (type, "ClassSetter: Unhandled type: %s, %s", type.getClass ().getName (), type);
	}
    }

    private void setTypes (List<ClassType> types) {
	if (types != null)
	    types.forEach (this::setType);
    }

    private void setType (ClassType ct) {
	if (ct.getFullName () != null) // already set?
	    return;
	// "List" and "java.util.List" are easy to resolve
	String id1 = getId (ct);
	ResolvedClass fqn = resolve (id1, ct.getPosition ());
	if (fqn == null && ct.size () > 1) {
	    // ok, someone probably wrote something like "HashMap.Entry" or
	    // "java.util.HashMap.Entry" which is somewhat problematic, but legal
	    fqn = tryAllParts (ct);
	}
	if (fqn == null) {
	    diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), ct.getPosition (),
							 "Failed to find class: %s", id1));
	    return;
	}
	ct.setFullName (fqn.type);
	ct.setTypeParameter (fqn.tp);
	for (SimpleClassType sct : ct.get ()) {
	    TypeArguments tas = sct.getTypeArguments ();
	    if (tas != null) {
		tas.getTypeArguments ().forEach (tn -> setType (tn));
	    }
	}
    }

    private String getId (ClassType ct) {
	if (ct.size () == 1)
	    return ct.get ().get (0).getId ();
	return ct.get ().stream ().map (s -> s.getId ()).collect (Collectors.joining ("."));
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
	    String name = dn.getDotName ();
	    if (!hasVisibleType (name)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getPosition (),
							     "Imported type not found: %s", name));
		i.markUsed (); // Unused, but already flagged as bad, don't want multiple lines
	    }
	    ImportHolder prev = stid.put (dn.getLastPart (), new ImportHolder (i, name));
	    if (prev != null) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getPosition (),
							     "Name clash for import: %s", dn.getLastPart ()));
	    }
	}

	private void addSingleStaticImportDeclaration (SingleStaticImportDeclaration i) {
	    String fqn = i.getName ().getDotName ();
	    if (!hasVisibleType (fqn)) {
		diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), i.getPosition (),
							     "Imported type not found: %s", fqn));
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

    private boolean sameTopLevelClass (String fqn, String topLevelClass) {
	return fqn.length () > topLevelClass.length () && fqn.startsWith (topLevelClass) &&
	    fqn.charAt (topLevelClass.length ()) == '.';
    }

    private static record  ImportHolder (ImportDeclaration i, String name) {
	public void markUsed () {
	    if (i != null)
		i.markUsed ();
	}
    }

    private boolean hasVisibleType (String fqn) {
	String topLevelClass = containingTypes == null ? null : lastFQN (containingTypes);
	return hasVisibleType (fqn, topLevelClass);
    }

    private String lastFQN (EnclosingTypes e) {
	String fqn = null;
	while (e != null) {
	    fqn = e.fqn ();
	    e = e.previous ();
	}
	return fqn;
    }

    private boolean hasVisibleType (String fqn, String topLevelClass) {
	LookupResult r = cip.hasVisibleType (fqn);
	if (!r.getFound ())
	    return false;
	if (Flags.isPublic (r.getAccessFlags ())) {
	    return true;
	} else if (Flags.isProtected (r.getAccessFlags ())) {
	    return samePackage (fqn, packageName) || insideSuperClass (fqn);
	} else if (Flags.isPrivate (r.getAccessFlags ())) {
	    return sameTopLevelClass (fqn, topLevelClass);
	}
	// No access level
	return samePackage (fqn, packageName);
    }

    private boolean samePackage (String fqn, String packageName) {
	String start = packageName == null ? "" : packageName;
	return fqn.length () > start.length () && fqn.startsWith (start) &&
	    fqn.indexOf ('.', fqn.length ()) == -1;
    }

    private boolean insideSuperClass (String fqn) {
	for (EnclosingTypes c : containingTypes)
	    if (insideSuperClass (fqn, c.fqn ()))
		return true;
	return false;
    }

    private boolean insideSuperClass (String fqn, String currentClass) {
	try {
	    Optional<List<String>> supers = cip.getSuperTypes (currentClass, false);
	    if (supers.isPresent ()) {
		for (String s :  supers.get ()) {
		    if (fqn.length () > s.length () && fqn.startsWith (s) &&
			fqn.charAt (s.length ()) == '.') {
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

    /** Try to find an outer class that has the inner classes for misdirected outer classes.
     */
    private ResolvedClass tryAllParts (ClassType ct) {
	StringBuilder sb = new StringBuilder ();
	List<SimpleClassType> scts = ct.get ();
	// group package names to class "java.util.HashMap"
	for (int i = 0, s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    if (i > 0)
		sb.append (".");
	    sb.append (sct.getId ());
	    ResolvedClass outerClass = resolve (sb.toString (), ct.getPosition ());
	    if (outerClass != null) {
		// Ok, now check if Entry is an inner class either directly or in super class
		String fqn = checkForInnerClasses (scts, i + 1, outerClass.type);
		if (fqn != null)
		    return new ResolvedClass (fqn);
	    }
	}
	return null;
    }

    private String checkForInnerClasses (List<SimpleClassType> scts, int i, String outerClass) {
	String currentOuterClass = outerClass;
	for (int s = scts.size (); i < s; i++) {
	    SimpleClassType sct = scts.get (i);
	    String directInnerClass = currentOuterClass + "." + sct.getId ();
	    if (hasVisibleType (directInnerClass)) {
		currentOuterClass = directInnerClass;
	    } else {
		currentOuterClass = checkSuperClasses (currentOuterClass, sct.getId ());
		if (currentOuterClass == null)
		    return null;
	    }
	}
	return currentOuterClass;
    }

    private ResolvedClass resolve (String id, ParsePosition pos) {
	TypeParameter tp = getTypeParameter (id);
	if (tp != null) {
	    return new ResolvedClass (tp);
	}

	if (hasVisibleType (id))
	    return new ResolvedClass (id);

	String fqn = resolveInnerClass (id);
	if (fqn == null)
	    fqn = resolveUsingImports (id, pos);
	if (fqn != null)
	    return new ResolvedClass (fqn);
	return null;
    }

    private static class ResolvedClass {
	public final String type;
	public final TypeParameter tp;

	public ResolvedClass (String type) {
	    this.type = type;
	    this.tp = null;
	}

	public ResolvedClass (TypeParameter tp) {
	    this.type = tp.getExpressionType ().getClassName ();
	    this.tp = tp;
	}

	@Override public String toString () {
	    return "ResolvedClass{type: " + type + ", tp: " + tp + "}";
	}
    }

    private String resolveInnerClass (String id) {
	// Check for inner class
	for (EnclosingTypes ctn : containingTypes) {
	    String icn = ctn.fqn () + "." + id;
	    if (hasVisibleType (icn))
		return icn;
	}

	// Check for inner class of super classes
	for (EnclosingTypes ctn : containingTypes) {
	    String fqn = checkSuperClasses (ctn.fqn (), id);
	    if (fqn != null)
		return fqn;
	}
	return null;
    }

    private String checkSuperClasses (String fullCtn, String id) {
	if (fullCtn == null)
	    return null;
	List<String> superclasses = getSuperClasses (fullCtn);
	for (String superclass : superclasses) {
	    String icn = superclass + "." + id;
	    if (hasVisibleType (icn))
		return icn;
	    String ssn = checkSuperClasses (superclass, id);
	    if (ssn != null)
		return ssn;
	}
	return null;
    }

    private List<String> getSuperClasses (String type) {
	try {
	    Optional<List<String>> supers = cip.getSuperTypes (type, false);
	    return supers.isPresent () ? supers.get () : Collections.emptyList ();
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to load class: " + type, e));
	}
	return Collections.emptyList ();
    }

    private String resolveUsingImports (String id, ParsePosition pos) {
	ImportHolder i = ih.stid.get (id);
	if (i != null && hasVisibleType (i.name ())) {
	    i.markUsed ();
	    return i.name ();
	}
	String fqn;
	fqn = tryPackagename (id, pos);
	if (fqn != null && hasVisibleType (fqn))
	    return fqn;
	fqn = trySingleStaticImport (id);
	if (fqn != null && hasVisibleType (fqn))
	    return fqn;
	fqn = tryTypeImportOnDemand (id, pos);
	if (fqn != null) // already checked cip.hasType
	    return fqn;
	fqn = tryStaticImportOnDemand (id);
	if (fqn != null) // already checked cip.hasType
	    return fqn;

	return null;
    }

    private TypeParameter getTypeParameter (String id) {
	TypeHolder th = currentTypes;
	while (th != null) {
	    TypeParameter tp = th.types.get (id);
	    if (tp != null)
		return tp;
	    th = th.parent;
	}
	return null;
    }

    private String tryPackagename (String id, ParsePosition pos) {
	if (packageName == null)
	    return null;
	return packageName + "." + id;
    }

    private String tryTypeImportOnDemand (String id, ParsePosition pos) {
	List<String> matches = new ArrayList<> ();
	for (ImportHolder ih : ih.tiod) {
	    String t = ih.name () + "." + id;
	    if (hasVisibleType (t)) {
		matches.add (t);
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

    private String trySingleStaticImport (String id) {
	ImportHandler.StaticImportType sit = ih.ssid.get (id);
	if (sit == null)
	    return null;
	sit.ssid.markUsed ();
	return sit.fullName;
    }

    private String tryStaticImportOnDemand (String id) {
	for (StaticImportOnDemandDeclaration siod : ih.siod) {
	    String fqn = siod.getName ().getDotName () + "." + id;
	    if (hasVisibleType (fqn)) {
		siod.markUsed ();
		return fqn;
	    }
	}
	return null;
    }

    private static class TypeHolder {
	private final TypeHolder parent;
	private final Map<String, TypeParameter> types;

	public TypeHolder (TypeHolder parent, Map<String, TypeParameter> types) {
	    this.parent = parent;
	    this.types = types;
	}
    }

    public void checkUnusedImport () {
	List<ImportDeclaration> imports = ocu.getImports ();
	imports.stream ().filter (i -> !i.hasBeenUsed ()).forEach (i -> checkUnusedImport (i));
    }

    private void checkUnusedImport (ImportDeclaration i) {
	if (i instanceof SingleStaticImportDeclaration) {
	    SingleStaticImportDeclaration si = (SingleStaticImportDeclaration)i;
	    String full = si.getFullName ();
	    String fqn = si.getType ().getDotName ();
	    String field = si.getInnerId ();
	    if (!hasVisibleType (full) && cip.getFieldInformation (fqn, field) == null) {
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
	return new EnclosingTypes (previous, td, cip.getFullDotClassName (td));
    }

    private record EnclosingTypes (EnclosingTypes previous, TypeDeclaration td, String fqn)
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
