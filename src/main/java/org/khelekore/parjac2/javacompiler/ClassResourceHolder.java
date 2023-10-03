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
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

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
	storeName (name, ".sig", '/', new CtSymResult (ctSymFile, versions, moduleName, name, e.getName ()));
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

    public LookupResult hasVisibleType (String fqn) {
	ClasspathClassInformation r = foundClasses.get (fqn);
	if (r != null)
	    if (loadNoCheckedException (fqn, r))
		return new LookupResult (true, r.accessFlags);
	return LookupResult.NOT_FOUND;
    }

    public Optional<List<String>> getSuperTypes (String fqn) throws IOException {
	ClasspathClassInformation r = foundClasses.get (fqn);
	if (r == null)
	    return Optional.empty ();
	loadNoCheckedException (fqn, r);
	return Optional.of (r.superTypes);
    }

    private boolean loadNoCheckedException (String fqn, ClasspathClassInformation r) {
	try {
	    r.ensureNodeIsLoaded (fqn);
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
	private boolean loaded = false;
	private String fqn;
	private String superClass;
	private List<String> superTypes;
	private int accessFlags;

	// TODO: implement
	//private Map<String, AsmField> fieldTypes = new HashMap<> ();
	//private Map<String, List<MethodInformation>> methods = new HashMap<> ();

	public synchronized void ensureNodeIsLoaded (String fqn) throws IOException {
	    if (loaded)
		return;
	    loaded = true;
	    this.fqn = fqn;
	    readNode ();
	}

	public abstract void readNode () throws IOException;

	protected void readNode (InputStream is) throws IOException {
	    try {
		ClassReader cr = new ClassReader (is);
		ClassInfoExtractor cie = new ClassInfoExtractor (this);
		cr.accept (cie, ClassReader.SKIP_CODE);
		// TODO; implement
		// methods = Collections.unmodifiableMap (methods);
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

	public CtSymResult (Path ctSymFile, String versions, String moduleName, String name, String path) {
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
		    readNode (jis);
		}
	    }
	}

	@Override public String getPath () {
	    return path;
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

	@Override public void readNode () throws IOException {
	    try (InputStream is = Files.newInputStream (path)) {
		readNode (is);
	    }
	}

	@Override public String getPath () {
	    return path.toString ();
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

	@Override public void readNode () throws IOException {
	    // I tried to cache the jarfiles, but that made things a lot slower
	    // due to less multi threading. Do if someone can prove it makes sense.
	    try (JarFile jf = new JarFile (jarfile.toFile ())) {
		JarEntry e = jf.getJarEntry (path);
		try (InputStream jis = jf.getInputStream (e)) {
		    readNode (jis);
		}
	    }
	}

	@Override public String getPath () {
	    return jarfile + "!" + path;
	}
    }

    private static class ClassInfoExtractor extends ClassVisitor {
	private final ClasspathClassInformation r;

	public ClassInfoExtractor (ClasspathClassInformation r) {
	    super (Opcodes.ASM9);
	    this.r = r;
	}

	@Override public void visit (int version, int access, String name,
				     String signature, String superName,
				     String[] interfaces) {
	    if (superName != null) // java.lang.Object have null superName
		r.superClass = superName.replace ('/', '.');
	    r.accessFlags = access;
	    int size = 0;
	    if (r.superClass != null)
		size++;
	    if (interfaces != null)
		size += interfaces.length;
	    r.superTypes = new ArrayList<> (size);
	    if (r.superClass != null)
		r.superTypes.add (r.superClass);
	    if (interfaces != null) {
		for (String i : interfaces)
		    r.superTypes.add (i.replace ('/', '.'));
	    }
	}

	@Override public void visitSource (String source, String debug) {
	}

	@Override public void visitEnd () {
	}

	@Override public void visitInnerClass (String name, String outerName,
					       String innerName, int access) {
	}

	@Override public FieldVisitor visitField (int access, String name, String desc,
						  String signature, Object value) {
	    // desc is type, signature is only there if field is generic type
	    Type type = Type.getType (desc);
	    // TODO: implement field extraction
	    // r.fieldTypes.put (name, new AsmField (access, ExpressionType.get (type), signature, value));
	    return null;
	}

	@Override public MethodVisitor visitMethod (int access, String name,
						    String desc, String signature,
						    String[] exceptions) {
	    /* TODO: implement
	    // desc is type, signature is only there if field is generic type
	    MethodInformation mi = new MethodInformation (r.fqn, access, name, desc, signature, exceptions);
	    List<MethodInformation> ls = r.methods.get (name);
	    if (ls == null) {
		ls = new ArrayList<> ();
		r.methods.put (name, ls);
	    }
	    ls.add (mi);
	    */
	    return null;
	}
    }

    /* TODO: implement
    private static class AsmField implements Flagged {
	private final int access;
	private final ExpressionType type;
	private final String signature;
	private final Object value;

	public AsmField (int access, ExpressionType type, String signature, Object value) {
	    this.access = access;
	    this.type = type;
	    this.signature = signature;
	    this.value = value;
	}

	@Override public int getFlags () {
	    return access;
	}

	@Override public List<ParseTreeNode> getAnnotations () {
	    return Collections.emptyList ();
	}

	@Override public ParsePosition getParsePosition() {
	    return null;
	}

	@Override public ExpressionType getExpressionType () {
	    return type;
	}

	@Override public String toString () {
	    return getClass ().getSimpleName () + "{" + access + ", " + type + ", " +
		signature + ", " + value + "}";
	}
    }
    */
}

