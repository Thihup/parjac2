package org.khelekore.parjac2.javacompiler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.ModularCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.ModuleDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalClassDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalInterfaceDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.RecordDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.UnqualifiedClassInstanceCreationExpression;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class CompiledTypesHolder {

    // full class name to declaration, name only has '.' as separator, no / or $
    // foo.bar.Baz.Qas -> TD
    private Map<String, TypeDeclaration> foundClasses = new ConcurrentHashMap<> ();
    private Map<TypeDeclaration, TypeInfo> typeToInfo = new ConcurrentHashMap<> ();
    private Map<String, ModuleDeclaration> foundModules = new ConcurrentHashMap<> ();

    public LookupResult hasVisibleType (String fqn) {
	TypeDeclaration ni = foundClasses.get (fqn);
	if (ni == null)
	    return LookupResult.NOT_FOUND;
	int flags = ni.getFlags ();
	return new LookupResult (true, flags);
    }

    public void addTypes (ParseTreeNode n, Path origin) {
	// We get OrdinaryCompilationUnit or ModularCompilationUnit
	if (n instanceof OrdinaryCompilationUnit ocu) {
	    String packageName = ocu.getPackageName ();
	    for (TypeDeclaration td : ocu.getTypes ()) {
		addType (packageName, packageName, "", "", td, origin);
	    }
	} else if (n instanceof ModularCompilationUnit mcu) {
	    // TODO: not sure if dotted name is correct here
	    foundModules.put (mcu.getModule ().getDottedName (), mcu.getModule ());
	}
    }

    private void addType (String packageName, String namePrefix,
			  String dotPrefix, String dollarPrefix,
			  TypeDeclaration td, Path origin) {
	String fullName = namePrefix.isEmpty () ? td.getName () : (namePrefix + "." + td.getName ());
	foundClasses.put (fullName, td);
	String dotName = dotPrefix.isEmpty () ? td.getName () : (dotPrefix + "." + td.getName ());
	String dollarName = dollarPrefix.isEmpty () ? td.getName () : (dollarPrefix + "$" + td.getName ());
	typeToInfo.put (td, new TypeInfo (packageName, dotName, dollarName, origin));
	td.getInnerClasses ().forEach (i -> addType (packageName, fullName, dotName, dollarName, i, origin));
	td.getInnerClasses ().forEach (i -> i.setOuterClass (td));
    }

    public int getCompiledClassCount () {
	return foundClasses.size ();
    }

    public int getCompiledModuleCount () {
	return foundModules.size ();
    }

    public Collection<TypeDeclaration> getCompiledClasses () {
	return foundClasses.values ();
    }

    public Collection<ModuleDeclaration> getCompiledModules () {
	return foundModules.values ();
    }

    public String getPackageName (TypeDeclaration td) {
	return typeToInfo.get (td).packageName;
    }

    public String getFullDotClassName (TypeDeclaration td) {
	TypeInfo info = typeToInfo.get (td);
	return info.getFullDotClassName ();
    }

    public String getFullDollarClassName (TypeDeclaration td) {
	TypeInfo info = typeToInfo.get (td);
	return info.getFullDollarClassName ();
    }

    public String getFileName (TypeDeclaration td) {
	return typeToInfo.get (td).dollarName;
    }

    public Path getOriginFile (TypeDeclaration td) {
	return typeToInfo.get (td).origin;
    }

    public Optional<List<FullNameHandler>> getSuperTypes (String type) {
	TypeDeclaration tn = getType (type);

	return switch (tn) {
	case NormalClassDeclaration ncd -> getSuperTypes (ncd);
	case NormalInterfaceDeclaration nid -> getSuperTypes (nid);
	case UnqualifiedClassInstanceCreationExpression at -> getSuperTypes (at);
	case EnumDeclaration ed -> getSuperTypes (ed);
	case RecordDeclaration rd -> getSuperTypes (rd);
	case null, default -> Optional.empty ();
	};
    }

    private Optional<List<FullNameHandler>> getSuperTypes (NormalClassDeclaration ncd) {
	List<FullNameHandler> ret = new ArrayList<> ();
	ClassType ct = ncd.getSuperClass ();
	if (ct != null) {
	    if (!ct.hasFullName ())
		return Optional.of (Collections.<FullNameHandler>emptyList ());
	    ret.add (ct.getFullNameHandler ());
	} else {
	    ret.add (FullNameHandler.JL_OBJECT);
	}
	addInterfaces (ret, ncd.getSuperInterfaces ());
	return Optional.of (ret);
    }

    private Optional<List<FullNameHandler>> getSuperTypes (NormalInterfaceDeclaration nid) {
	List<ClassType> cts = nid.getExtendsInterfaces ();
	if (cts != null) {
	    List<FullNameHandler> ls = new ArrayList<> (cts.size ());
	    addInterfaces (ls, cts);
	    return Optional.of (ls);
	}
	return Optional.empty ();
    }

    private Optional<List<FullNameHandler>> getSuperTypes (UnqualifiedClassInstanceCreationExpression at) {
	ClassType ct = at.getSuperType ();
	if (ct != null) {
	    List<FullNameHandler> ret = Arrays.asList (ct.getFullNameHandler ());
	    return Optional.of (ret);
	}
	return Optional.empty ();
    }

    private Optional<List<FullNameHandler>> getSuperTypes (EnumDeclaration ed) {
	List<FullNameHandler> ret = new ArrayList<> ();
	ret.add (FullNameHandler.JL_ENUM);
	addInterfaces (ret, ed.getSuperInterfaces ());
	return Optional.of (ret);
    }

    private Optional<List<FullNameHandler>> getSuperTypes (RecordDeclaration rd) {
	List<FullNameHandler> ret = new ArrayList<> ();
	ret.add (FullNameHandler.JL_RECORD);
	addInterfaces (ret, rd.getSuperInterfaces ());
	return Optional.of (ret);
    }

    /** Get the outer tree node for a given fully qualified name,
     *  that is "some.package.Foo$Bar".
     */
    public TypeDeclaration getType (String fqn) {
	return foundClasses.get (fqn);
    }

    private void addInterfaces (List<FullNameHandler> ret, List<ClassType> types) {
	if (types != null)
	    types.forEach (ct -> ret.add (ct.getFullNameHandler ()));
    }

    private static class TypeInfo {
	private final String packageName;  // "foo.bar"
	private final String dotName;      // Baz.Qaz
	private final String dollarName;   // Baz$Qaz
	private final Path origin;         // foo/bar/Baz.java

	private String fullDotName;        // foo.bar.Baz.Qaz
	private String fullDollarName;     // foo.bar.Baz$Qaz

	public TypeInfo (String packageName, String dotName, String dollarName, Path origin) {
	    this.packageName = packageName;
	    this.dotName = dotName;
	    this.dollarName = dollarName;
	    this.origin = origin;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + packageName + ", " + dotName + ", " + dollarName + ", " +
		origin + ", " + fullDotName + ", " + fullDollarName + "}";
	}

	private synchronized String getFullDotClassName () {
	    if (fullDotName == null) {
		String pn = packageName;
		String cn = dotName;
		fullDotName = pn.isEmpty () ? cn : pn + "." + cn;
	    }
	    return fullDotName;
	}

	private synchronized String getFullDollarClassName () {
	    if (fullDollarName == null) {
		String pn = packageName;
		String cn = dollarName;
		fullDollarName = pn.isEmpty () ? cn : pn + "." + cn;
	    }
	    return fullDollarName;
	}
    }
}
