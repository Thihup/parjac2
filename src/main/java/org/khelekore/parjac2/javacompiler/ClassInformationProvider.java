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

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.ModuleDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeBound;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ClassInformationProvider {
    private final CompilerDiagnosticCollector diagnostics;
    private final ClassResourceHolder crh;
    private final CompiledTypesHolder cth;
    private Map<TypeParameter, String> typeToName;
    private Map<String, TypeParameter> nameToType;

    public ClassInformationProvider (CompilerDiagnosticCollector diagnostics, CompilationArguments settings) {
	this.diagnostics = diagnostics;
	crh = new ClassResourceHolder (diagnostics, settings);
	cth = new CompiledTypesHolder ();

	typeToName = new ConcurrentHashMap<> ();
	nameToType = new ConcurrentHashMap<> ();
    }

    public LookupResult hasVisibleType (String fqn) {
	LookupResult res = cth.hasVisibleType (fqn);
	if (res.getFound ())
	    return res;
	return crh.hasVisibleType (fqn);
    }


    public Optional<List<String>> getSuperTypes (String fqn, boolean isArray) throws IOException {
	if (isArray)
	    return Optional.of (Collections.singletonList ("java.lang.Object"));
	Optional<List<String>> supers = cth.getSuperTypes (fqn);
	if (supers.isPresent ())
	    return supers;
	TypeParameter tp = nameToType.get (fqn);
	if (tp != null)
	    return Optional.of (getSuperTypes (tp));
	return crh.getSuperTypes (fqn);
    }

    private List<String> getSuperTypes (TypeParameter tp) {
	TypeBound b = tp.getTypeBound ();
	if (b == null)
	    return Collections.singletonList ("java.lang.Object");
	List<String> ret = new ArrayList<> (b.size ());
	ret.add (b.getType ().getFullName ());
	List<ClassType> ls = b.getAdditionalBounds ();
	if (ls != null) {
	    for (ClassType ab : ls)
		ret.add (ab.getFullName ());
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

    public String getFullClassName (TypeDeclaration td) {
	return cth.getFullClassName (td);
    }

    public String getFileName (TypeDeclaration td) {
	return cth.getFileName (td);
    }

    public Path getOriginFile (TypeDeclaration td) {
	return cth.getOriginFile (td);
    }

    public FieldInformation<?> getFieldInformation (String fqn, String field) {
	return null; // TODO: implement
    }
}
