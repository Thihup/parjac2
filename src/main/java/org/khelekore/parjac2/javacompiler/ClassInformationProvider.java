package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.ModuleDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalInterfaceDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeBound;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ClassInformationProvider {
    private final CompilerDiagnosticCollector diagnostics;
    private final ClassResourceHolder crh;
    private final CompiledTypesHolder cth;
    private Map<String, TypeParameter> nameToType;

    public ClassInformationProvider (CompilerDiagnosticCollector diagnostics, CompilationArguments settings) {
	this.diagnostics = diagnostics;
	crh = new ClassResourceHolder (diagnostics, settings);
	cth = new CompiledTypesHolder ();
	nameToType = new ConcurrentHashMap<> ();
    }

    public LookupResult hasVisibleType (String dottedName) {
	LookupResult res = cth.hasVisibleType (dottedName);
	if (res.found ())
	    return res;
	return crh.hasVisibleType (dottedName);
    }


    public List<FullNameHandler> getSuperTypes (String fqn, boolean isArray) throws IOException {
	if (isArray)
	    return List.of (FullNameHandler.JL_OBJECT);
	Optional<List<FullNameHandler>> supers = cth.getSuperTypes (fqn);
	if (supers.isPresent ())
	    return supers.get ();
	TypeParameter tp = nameToType.get (fqn);
	if (tp != null)
	    return getSuperTypes (tp);
	return crh.getSuperTypes (fqn);
    }

    private List<FullNameHandler> getSuperTypes (TypeParameter tp) {
	TypeBound b = tp.getTypeBound ();
	if (b == null)
	    return Collections.singletonList (FullNameHandler.JL_OBJECT);
	List<FullNameHandler> ret = new ArrayList<> (b.size ());
	ret.add (b.getType ().fullName ());
	List<ClassType> ls = b.getAdditionalBounds ();
	if (ls != null) {
	    for (ClassType ab : ls)
		ret.add (ab.fullName ());
	}
	return ret;
    }

    public void scanClassPath () {
	try {
	    crh.scanClassPath ();
	} catch (IOException e) {
	    diagnostics.report (new NoSourceDiagnostics ("Failed to scan classpath: %s", e.toString ()));
	}
    }

    public int getClasspathEntrySize () {
	return crh.getClasspathEntrySize ();
    }

    public void addTypes (ParseTreeNode n, Path origin) {
	cth.addTypes (n, origin);
    }

    public int getCompiledClassCount () {
	return cth.getCompiledClassCount ();
    }

    public int getCompiledModuleCount () {
	return cth.getCompiledModuleCount ();
    }

    public Collection<TypeDeclaration> getCompiledClasses () {
	return cth.getCompiledClasses ();
    }

    public Collection<ModuleDeclaration> getCompiledModules () {
	return cth.getCompiledModules ();
    }

    public String getPackageName (TypeDeclaration td) {
	return cth.getPackageName (td);
    }

    public FullNameHandler getFullName (TypeDeclaration td) {
	return cth.getFullName (td);
    }

    public String getFileName (TypeDeclaration td) {
	return cth.getFileName (td);
    }

    public Path getOriginFile (TypeDeclaration td) {
	return cth.getOriginFile (td);
    }

    public VariableInfo getFieldInformation (FullNameHandler fqn, String field) {
	return tryActions (fqn,
			   td -> td.getFields ().get (field),
			   dn -> crh.getFieldInformation (dn, field));
    }

    public List<MethodInfo> getMethods (FullNameHandler fqn, String methodName) {
	return tryActions (fqn,
			   td -> td.getMethodInformation (fqn, methodName),
			   dn -> crh.getMethodInformation (dn, methodName));
    }

    public Map<String, List<MethodInfo>> getMethods (FullNameHandler fqn) {
	return tryActions (fqn,
			   td -> td.getMethodInformation (fqn),
			   dn -> crh.getMethodInformation (dn));
    }

    public boolean isInterface (String fqn) {
	TypeDeclaration td = cth.getType (fqn);
	if (td instanceof NormalInterfaceDeclaration)
	    return true;
	return crh.isInterface (fqn);
    }

    private <R> R tryActions (FullNameHandler fqn, Function<TypeDeclaration, R> tdFunc, Function<String, R> crhFunc) {
	if (fqn == null)
	    throw new NullPointerException ("null is not a valid class name");
	String dotName = fqn.getFullDotName ();
	TypeDeclaration td = cth.getType (dotName);
	if (td != null)
	    return tdFunc.apply (td);
	return crhFunc.apply (dotName);
    }
}
