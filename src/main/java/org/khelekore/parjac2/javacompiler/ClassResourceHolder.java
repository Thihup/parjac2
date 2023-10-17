package org.khelekore.parjac2.javacompiler;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.NoSourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;

import io.github.dmlloyd.classfile.ClassModel;
import io.github.dmlloyd.classfile.Classfile;
import io.github.dmlloyd.classfile.constantpool.ClassEntry;

public class ClassResourceHolder {
    private final CompilerDiagnosticCollector diagnostics;
    private final Path ctSym;
    private final List<Path> classPathEntries;

    // full class name to information, name only has '.' as separator, no / or $
    private Map<String, ClasspathClassInformation> foundClasses = new HashMap<> ();

    public ClassResourceHolder (CompilerDiagnosticCollector diagnostics, CompilationArguments settings) {
	this.diagnostics = diagnostics;
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
	    jf.stream ().forEach (e -> storeCtSymEntry (ctSym, e, ctSymVersion));
	}
    }

    private void storeCtSymEntry (Path ctSymFile, JarEntry e, String ctSymVersion) {
	String name = e.getName ();
	String original = name;
	if (name.startsWith ("meta-inf/"))
	    return; // skip
	if (!name.endsWith (".sig"))
	    return; // skip
	int i = name.indexOf ("/");
	if (i < 0)
	    diagnostics.report (new NoSourceDiagnostics ("ct.sym entry: %s does not contain any path, skipping",
							 name));

	String versions = name.substring (0, i);
	name = name.substring (i + 1);
	i = name.indexOf ('/');
	if (i < 0)
	    diagnostics.report (new NoSourceDiagnostics ("ct.sym entry: %s does not contain any module, skipping",
							 original));
	String moduleName = name.substring (0, i);
	name = name.substring (i + 1);

	// For now we are going to assume that later version come later in the file so
	// no need to check ctSymVersion
	FullNameHandler fullName = getFullName (name, ".sig", '/');
	storeName (new CtSymResult (fullName, ctSymFile, versions, moduleName, name, e.getName ()));
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
		    FullNameHandler fullName = getFullName (relative.toString (), ".class", File.separatorChar);
		    storeName (new PathResult (fullName, f));
		}
	    });
    }

    private void scanJar (Path jarfile) throws IOException {
	try (JarFile jf = new JarFile (jarfile.toFile ())) {
	    jf.stream ().forEach (e -> storeClass (jarfile, e));
	}
    }

    private void storeClass (Path jarfile, JarEntry e) {
	JarEntryResult r = new JarEntryResult (getFullName (e.getName (), ".class", '/'), jarfile, e.getName ());
	storeName (r);
    }

    private static FullNameHandler getFullName (String name, String extension, char dirSeparator) {
	name = name.substring (0, name.length () - extension.length ());
	String slashName = name.replace (dirSeparator, '/');
	String dollarName = slashName.replace ('/', '.');
	String dotName = dollarName.replace ('$', '.');
	return FullNameHandler.of (dotName, dollarName);
    }

    private void storeName (ClasspathClassInformation r) {
	foundClasses.put (r.getFullDotName (), r);
    }

    public LookupResult hasVisibleType (String fqn) {
	ClasspathClassInformation r = foundClasses.get (fqn);
	if (r != null)
	    if (loadNoCheckedException (r))
		return new LookupResult (true, r.accessFlags, r.getFullName ());
	return LookupResult.NOT_FOUND;
    }

    public Optional<List<FullNameHandler>> getSuperTypes (String fqn) throws IOException {
	ClasspathClassInformation r = foundClasses.get (fqn);
	if (r == null)
	    return Optional.empty ();
	loadNoCheckedException (r);
	return Optional.of (r.superTypes);
    }

    public boolean isInterface (String fqn) {
	ClasspathClassInformation r = foundClasses.get (fqn);
	if (r == null)
	    return false;
	loadNoCheckedException (r);
	return Flags.isInterface (r.accessFlags);
    }

    private boolean loadNoCheckedException (ClasspathClassInformation r) {
	try {
	    r.ensureNodeIsLoaded ();
	    return true;
	} catch (IOException e) {
	    e.printStackTrace ();
	    diagnostics.report (new NoSourceDiagnostics ("Failed to load class from: " +
							 r.getPath () + ", " + e));
	}
	return false;
    }

    public int getClasspathEntrySize () {
	return foundClasses.size ();
    }

    private static abstract class ClasspathClassInformation {
	private FullNameHandler fullName;
	private boolean loaded = false;
	private FullNameHandler superClass;
	private List<FullNameHandler> superTypes;
	private int accessFlags;

	public ClasspathClassInformation (FullNameHandler fullName) {
	    this.fullName = fullName;
	}

	public FullNameHandler getFullName () {
	    return fullName;
	}

	public String getFullDotName () {
	    return fullName.getFullDotName ();
	}

	// TODO: implement
	//private Map<String, AsmField> fieldTypes = new HashMap<> ();
	//private Map<String, List<MethodInformation>> methods = new HashMap<> ();

	public synchronized void ensureNodeIsLoaded () throws IOException {
	    if (loaded)
		return;
	    loaded = true;
	    readNode ();
	}

	public abstract void readNode () throws IOException;

	protected void readNode (byte[] data) throws IOException {
	    try {
		ClassModel model = Classfile.of ().parse (data);
		ClassInfoExtractor cie = new ClassInfoExtractor (this, model);
		cie.parse ();
	    } catch (RuntimeException e) {
		throw new IOException ("Failed to read class: ", e);
	    }
	}

	public abstract String getPath ();
    }

    private static class CtSymResult extends ClasspathClassInformation {
	private final Path ctSymFile;
	private final String versions;
	private final String moduleName;
	private final String name;

	// can be something like "K/jdk.xml.dom/org/w3c/dom/xpath/XPathEvaluator.sig"
	private final String path;

	public CtSymResult (FullNameHandler fullName,
			    Path ctSymFile, String versions, String moduleName,
			    String name, String path) {
	    super (fullName);
	    this.ctSymFile = ctSymFile;
	    this.versions = versions;
	    this.moduleName = moduleName;
	    this.name = name;
	    this.path = path;
	}

	@Override public String toString () {
	    return getClass ().getName () + "{" + path + "}";
	}

	@Override public void readNode () throws IOException {
	    try (JarFile jf = new JarFile (ctSymFile.toFile ())) {
		JarEntry e = jf.getJarEntry (path);
		try (InputStream jis = jf.getInputStream (e)) {
		    readNode (jis.readAllBytes ());
		}
	    }
	}

	@Override public String getPath () {
	    return path;
	}
    }

    private static class PathResult extends ClasspathClassInformation {
	private final Path path;

	public PathResult (FullNameHandler fullName, Path path) {
	    super (fullName);
	    this.path = path;
	}

	@Override public String toString () {
	    return getClass ().getName () + "{" + path + "}";
	}

	@Override public void readNode () throws IOException {
	    readNode (Files.readAllBytes (path));
	}

	@Override public String getPath () {
	    return path.toString ();
	}
    }

    private static class JarEntryResult extends ClasspathClassInformation {
	private final Path jarfile;
	private final String path;

	public JarEntryResult (FullNameHandler fullName, Path jarfile, String path) {
	    super (fullName);
	    this.jarfile = jarfile;
	    this.path = path;
	}

	@Override public String toString () {
	    return getClass ().getName () + "{" + jarfile + "!" + path + "}";
	}

	@Override public void readNode () throws IOException {
	    // I tried to cache the jarfiles, but that made things a lot slower
	    // due to less multi threading. Do if someone can prove it makes sense.
	    try (JarFile jf = new JarFile (jarfile.toFile ())) {
		JarEntry e = jf.getJarEntry (path);
		try (InputStream jis = jf.getInputStream (e)) {
		    readNode (jis.readAllBytes ());
		}
	    }
	}

	@Override public String getPath () {
	    return jarfile + "!" + path;
	}
    }

    private static class ClassInfoExtractor {
	private final ClasspathClassInformation r;
	private final ClassModel model;

	public ClassInfoExtractor (ClasspathClassInformation r, ClassModel model) {
	    this.r = r;
	    this.model = model;
	}

	public void parse () {
	    parseAccessFlags ();
	    parseSuperTypes ();
	}

	private void parseAccessFlags () {
	    r.accessFlags = model.flags ().flagsMask ();
	}

	private void parseSuperTypes () {
	    Optional<ClassEntry> s = model.superclass ();
	    s.ifPresent (e -> r.superClass = getFullName (e));

	    int size = 0;
	    if (r.superClass != null)
		size++;
	    List<ClassEntry> interfaces = model.interfaces ();
	    size += interfaces.size ();
	    r.superTypes = new ArrayList<> (size);
	    if (r.superClass != null)
		r.superTypes.add (r.superClass);
	    if (interfaces != null) {
		for (ClassEntry ce : interfaces)
		    r.superTypes.add (getFullName (ce));
	    }
	}

	private FullNameHandler getFullName (ClassEntry ce) {
	    String slashName = ce.name ().stringValue ();
	    String dollarName = slashName.replace ('/', '.');
	    String dotName = dollarName.replace ('$', '.');
	    return FullNameHandler.of (dotName, dollarName);
	}
    }
}
