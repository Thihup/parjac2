package org.khelekore.parjac2.javacompiler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.ModularCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.ModuleDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ClassInformationProvider {
    private final CompilerDiagnosticCollector diagnostics;
    private final ClassResourceHolder crh;
    private final CompiledTypesHolder cth;

    public ClassInformationProvider (CompilerDiagnosticCollector diagnostics, CompilationArguments settings) {
	this.diagnostics = diagnostics;
	crh = new ClassResourceHolder (settings);
	cth = new CompiledTypesHolder ();
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

    private class ClassResourceHolder {
	private final Path ctSym;
	private final List<Path> classPathEntries;

	// full class name to information, name only has '.' as separator, no / or $
	private Map<String, ClasspathClassInformation> foundClasses = new HashMap<> ();

	public ClassResourceHolder (CompilationArguments settings) {
	    this.classPathEntries = settings.getClassPathEntries ();
	    String javaHome = System.getProperty ("java.home");
	    ctSym = Paths.get (javaHome, "lib", "ct.sym");
	}

	public void scanClassPath () throws IOException {
	    scanCtSym ();
	    for (Path p : classPathEntries)
		scan (p);
	}

	private void scanCtSym () throws IOException {
	    String javaVersion = System.getProperty ("java.specification.version");
	    int v = Integer.parseInt (javaVersion);
	    if (v < 11)
		throw new IllegalStateException ("Unhandled java version: " + v + ", requires 11 or later");
	    String ctSymVersion = Integer.toString (v, 36); // 0-9, a-z
	    if (!Files.exists (ctSym))
		throw new IOException ("Failed to find: " + ctSym);

	    try (JarFile jf = new JarFile (ctSym.toFile ())) {
		    jf.stream ().forEach (e -> storeCtSymEntry (e, ctSymVersion));
		}
	}

	private void storeCtSymEntry (JarEntry e, String ctSymVersion) {
	    String name = e.getName ();
	    if (name.startsWith ("meta-inf/"))
		return; // skip
	    if (!name.endsWith (".sig"))
		return; // skip
	    int i = name.indexOf ("/");
	    if (i < 0)
		diagnostics.report (new NoSourceDiagnostics ("ct.sym entry: %s does not contain any path, skipping",
							     name));
	    name = name.substring (i + 1);
	    // For now we are going to assume that later version come later in the file so
	    // no need to check ctSymVersion
	    storeName (name, ".sig", '/', new CtSymResult (e.getName ()));
	}

	private void scan (Path p) throws IOException {
	    if (Files.isDirectory (p))
		scanDirectory (p);
	    else if (Files.isRegularFile (p))
		scanJar (p);
	}

	private void scanDirectory (final Path start) throws IOException {
	    Files.walk (start).forEach (f -> {
		    if (Files.isRegularFile (f)) {
			Path relative = start.relativize (f);
			storeName (relative.toString (), ".class", File.separatorChar, new PathResult (f));
		    }
		});
	}

	private void scanJar (Path jarfile) throws IOException {
	    try (JarFile jf = new JarFile (jarfile.toFile ())) {
		    jf.stream ().forEach (e -> storeClass (jarfile, e));
		}
	}

	private void storeClass (Path jarfile, JarEntry e) {
	    JarEntryResult r = new JarEntryResult (jarfile, e.getName ());
	    storeName (e.getName (), ".class", '/', r);
	}

	private void storeName (String name, String type, char dirSeparator, ClasspathClassInformation r) {
	    if (name.endsWith (type)) {
		name = name.substring (0, name.length () - type.length ());
		name = name.replace (dirSeparator, '.');
		name = name.replace ('$', '.');
		foundClasses.put (name, r);
	    }
	}

	public int getClasspathEntrySize () {
	    return foundClasses.size ();
	}
    }

    private static abstract class ClasspathClassInformation {
	// TODO: implement
    }

    private static class CtSymResult extends ClasspathClassInformation {
	private final String path;

	public CtSymResult (String path) {
	    this.path = path;
	}

	@Override public String toString () {
	    return getClass ().getName () + "{" + path + "}";
	}
    }

    private static class PathResult extends ClasspathClassInformation {
	private final Path path;

	public PathResult (Path path) {
	    this.path = path;
	}

	@Override public String toString () {
	    return getClass ().getName () + "{" + path + "}";
	}
    }

    private static class JarEntryResult extends ClasspathClassInformation {
	private final Path jarfile;
	private final String path;

	public JarEntryResult (Path jarfile, String path) {
	    this.jarfile = jarfile;
	    this.path = path;
	}

	@Override public String toString () {
	    return getClass ().getName () + "{" + jarfile + "!" + path + "}";
	}
    }

    private class CompiledTypesHolder {

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
	    String tdName = td.isLocalClass () ? td.getLocalName () : td.getName ();
	    String fullName = namePrefix.isEmpty () ? tdName : (namePrefix + "." + tdName);
	    foundClasses.put (fullName, td);
	    typeToPackagename.put (td, packageName);
	    typeToOrigin.put (td, origin);
	    String className = classPrefix.isEmpty () ? tdName : (classPrefix + "$" + tdName);
	    typeToFullName.put (td, className);
	    List<TypeDeclaration> inners = td.getInnerClasses ();
	    inners.forEach (i -> addType (packageName, fullName, className, i, origin));
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
    }
}
