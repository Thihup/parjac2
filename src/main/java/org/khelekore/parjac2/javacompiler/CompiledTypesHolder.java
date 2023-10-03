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
import org.khelekore.parjac2.javacompiler.syntaxtree.Flagged;
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
    // TD -> foo.bar
    private Map<TypeDeclaration, String> typeToPackagename = new ConcurrentHashMap<> ();
    // TD -> Baz$Qaz
    private Map<TypeDeclaration, String> typeToFullName = new ConcurrentHashMap<> ();
    // TD -> foo/bar/Baz.java
    private Map<TypeDeclaration, Path> typeToOrigin = new ConcurrentHashMap<> ();
    private Map<String, ModuleDeclaration> foundModules = new ConcurrentHashMap<> ();

    public LookupResult hasVisibleType (String fqn) {
	TypeDeclaration ni = foundClasses.get (fqn);
	if (ni == null)
	    return LookupResult.NOT_FOUND;
	int flags = 0;
	if (ni instanceof Flagged ft)
	    flags = ft.getFlags ();
	return new LookupResult (true, flags);
    }

    public void addTypes (ParseTreeNode n, Path origin) {
	// We get OrdinaryCompilationUnit or ModularCompilationUnit
	if (n instanceof OrdinaryCompilationUnit ocu) {
	    String packageName = ocu.getPackageName ();
	    for (TypeDeclaration td : ocu.getTypes ()) {
		addType (packageName, packageName, "", td, origin);
	    }
	} else if (n instanceof ModularCompilationUnit mcu) {
	    // TODO: not sure if dotted name is correct here
	    foundModules.put (mcu.getModule ().getDottedName (), mcu.getModule ());
	}
    }

    private void addType (String packageName, String namePrefix, String classPrefix,
			  TypeDeclaration td, Path origin) {
	String fullName = namePrefix.isEmpty () ? td.getName () : (namePrefix + "." + td.getName ());
	foundClasses.put (fullName, td);
	typeToPackagename.put (td, packageName);
	typeToOrigin.put (td, origin);
	String className = classPrefix.isEmpty () ? td.getName () : (classPrefix + "$" + td.getName ());
	typeToFullName.put (td, className);
	td.getInnerClasses ().forEach (i -> addType (packageName, fullName, className, i, origin));
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
	return typeToPackagename.get (td);
    }

    public String getFullClassName (TypeDeclaration td) {
	String pn = getPackageName (td);
	String cn = typeToFullName.get (td);
	if (pn.isEmpty ())
	    return cn;
	return pn + "." + cn;
    }

    public String getFileName (TypeDeclaration td) {
	return typeToFullName.get (td);
    }

    public Path getOriginFile (TypeDeclaration td) {
	return typeToOrigin.get (td);
    }

    public Optional<List<String>> getSuperTypes (String type) {
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

    private Optional<List<String>> getSuperTypes (NormalClassDeclaration ncd) {
	List<String> ret = new ArrayList<> ();
	ClassType ct = ncd.getSuperClass ();
	if (ct != null) {
	    if (ct.getFullName () == null)
		return Optional.of (Collections.<String>emptyList ());
	    ret.add (ct.getFullName ());
	} else {
	    ret.add ("java.lang.Object");
	}
	addInterfaces (ret, ncd.getSuperInterfaces ());
	return Optional.of (ret);
    }

    private Optional<List<String>> getSuperTypes (NormalInterfaceDeclaration nid) {
	List<ClassType> cts = nid.getExtendsInterfaces ();
	if (cts != null) {
	    List<String> ls = new ArrayList<> ();
	    addInterfaces (ls, cts);
	    return Optional.of (ls);
	}
	return Optional.empty ();
    }

    private Optional<List<String>> getSuperTypes (UnqualifiedClassInstanceCreationExpression at) {
	ClassType ct = at.getSuperType ();
	if (ct != null) {
	    List<String> ret = Arrays.asList (ct.getFullName ());
	    return Optional.of (ret);
	}
	return Optional.empty ();
    }

    private Optional<List<String>> getSuperTypes (EnumDeclaration ed) {
	List<String> ret = new ArrayList<> ();
	ret.add ("java.lang.Enum");
	addInterfaces (ret, ed.getSuperInterfaces ());
	return Optional.of (ret);
    }

    private Optional<List<String>> getSuperTypes (RecordDeclaration rd) {
	List<String> ret = new ArrayList<> ();
	ret.add ("java.lang.Record");
	addInterfaces (ret, rd.getSuperInterfaces ());
	return Optional.of (ret);
    }

    /** Get the outer tree node for a given fully qualified name,
     *  that is "some.package.Foo$Bar".
     */
    public TypeDeclaration getType (String fqn) {
	return foundClasses.get (fqn);
    }

    private void addInterfaces (List<String> ret, List<ClassType> types) {
	if (types != null)
	    types.forEach (ct -> ret.add (ct.getFullName ()));
    }
}
