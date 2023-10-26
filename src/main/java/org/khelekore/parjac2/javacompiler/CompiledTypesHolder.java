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
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeArguments;
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
	int flags = ni.flags ();
	FullNameHandler name = typeToInfo.get (ni).getFullName ();
	return new LookupResult (true, flags, name);
    }

    public void addTypes (ParseTreeNode n, Path origin) {
	// We get OrdinaryCompilationUnit or ModularCompilationUnit
	if (n instanceof OrdinaryCompilationUnit ocu) {
	    String packageName = ocu.getPackageName ();
	    for (TypeDeclaration td : ocu.getTypes ()) {
		addType (packageName, null, td, origin);
	    }
	} else if (n instanceof ModularCompilationUnit mcu) {
	    // TODO: not sure if dotted name is correct here
	    foundModules.put (mcu.getModule ().getDottedName (), mcu.getModule ());
	}
    }

    private void addType (String packageName, PackageNameHandler outerClass,
			  TypeDeclaration td, Path origin) {
	PackageNameHandler fullName;
	if (outerClass == null) {
	    fullName = new TypeFullName (packageName, td);
	} else {
	    fullName = new InnerClassFullName (outerClass, td);
	}
	foundClasses.put (fullName.getFullDotName (), td);
	typeToInfo.put (td, new TypeInfo (fullName, origin));
	td.getInnerClasses ().forEach (i -> addType (packageName, fullName, i, origin));
	td.getInnerClasses ().forEach (i -> i.setOuterClass (td));
    }

    private interface PackageNameHandler extends FullNameHandler {
	String getPackageName ();
	void appendInternalSignature (StringBuilder sb, GenericTypeHelper gth,
				      ClassInformationProvider cip, boolean shortForm,
				      TypeArguments ta);
    }

    private record TypeFullName (String packageName, TypeDeclaration td) implements PackageNameHandler {
	@Override public String getFullDotName () {
	    return packageName.isEmpty () ? td.getName () : packageName + "." + td.getName ();
	}

	@Override public String getFullDollarName () {
	    return getFullDotName ();
	}

	@Override public boolean hasGenericType () {
	    return td.getTypeParameters () != null;
	}

	@Override public String getSignature (GenericTypeHelper gth, ClassInformationProvider cip,
					      boolean shortForm, TypeArguments ta) {
	    StringBuilder sb = new StringBuilder ();
	    appendInternalSignature (sb, gth, cip, shortForm, ta);
	    return sb.toString ();
	}

	@Override public String getPackageName () {
	    return packageName;
	}

	@Override public void appendInternalSignature (StringBuilder sb, GenericTypeHelper gth,
						       ClassInformationProvider cip, boolean shortForm,
						       TypeArguments ta) {
	    sb.append (getSlashName ());
	    if (ta != null)
		sb.append (gth.getTypeArgumentsSignature (ta, cip, shortForm));
	    else
		gth.appendTypeParametersSignature (sb, td.getTypeParameters (), cip, shortForm);
	}
    }

    private record InnerClassFullName (PackageNameHandler outer, TypeDeclaration td) implements PackageNameHandler {
	@Override public String getFullDotName () {
	    return outer.getFullDotName () + "." + td.getName ();
	}

	@Override public String getFullDollarName () {
	    return outer.getFullDollarName () + "$" + td.getName ();
	}

	@Override public boolean hasGenericType () {
	    return outer.hasGenericType () || td.getTypeParameters () != null;
	}

	@Override public String getSignature (GenericTypeHelper gth, ClassInformationProvider cip,
					      boolean shortForm, TypeArguments ta) {
	    StringBuilder sb = new StringBuilder();
	    outer.appendInternalSignature (sb, gth, cip, shortForm, ta);
	    appendInternalSignature (sb, gth, cip, shortForm, ta);
	    return sb.toString ();
	}

	@Override public void appendInternalSignature (StringBuilder sb, GenericTypeHelper gth,
						       ClassInformationProvider cip, boolean shortForm,
						       TypeArguments ta) {
	    sb.append (".").append (td.getName ());
	    if (ta != null)
		sb.append (gth.getTypeArgumentsSignature (ta, cip, shortForm));
	    else
		gth.appendTypeParametersSignature (sb, td.getTypeParameters (), cip, shortForm);
	}

	@Override public String getPackageName () {
	    return outer.getPackageName ();
	}
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
	return typeToInfo.get (td).getPackageName ();
    }

    public FullNameHandler getFullName (TypeDeclaration td) {
	TypeInfo info = typeToInfo.get (td);
	return info.getFullName ();
    }

    public String getFileName (TypeDeclaration td) {
	// from foo.barBar$Baz we want Bar$Baz
	return typeToInfo.get (td).getDollarName ();
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
	    List<FullNameHandler> ret = Arrays.asList (getFullNameHandler (ct));
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
     *  that is "some.package.Foo.Bar".
     */
    public TypeDeclaration getType (String fqn) {
	return foundClasses.get (fqn);
    }

    private void addInterfaces (List<FullNameHandler> ret, List<ClassType> types) {
	if (types != null)
	    types.forEach (ct -> ret.add (getFullNameHandler (ct)));
    }

    private FullNameHandler getFullNameHandler (ClassType ct) {
	FullNameHandler fnh = ct.getFullNameHandler ();
	if (fnh != null)
	    return fnh;
	return ct.getFullNameAsSimpleDottedName ();
    }

    private static class TypeInfo {
	private final PackageNameHandler fullName;
	private final Path origin;         // foo/bar/Baz.java

	public TypeInfo (PackageNameHandler fullName, Path origin) {
	    this.fullName = fullName;
	    this.origin = origin;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + fullName + ", " + origin + "}";
	}

	public FullNameHandler getFullName () {
	    return fullName;
	}

	public String getPackageName () {
	    return fullName.getPackageName ();
	}

	// from foo.barBar$Baz we want Bar$Baz
	public String getDollarName () {
	    String fullDollarName = fullName.getFullDollarName ();
	    String packageName = fullName.getPackageName ();
	    return packageName.isEmpty () ? fullDollarName : fullDollarName.substring (packageName.length () + 1);
	}
    }
}
