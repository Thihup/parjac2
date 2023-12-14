package org.khelekore.parjac2.javacompiler;

import java.util.Map;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclarationBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class NameModifierChecker extends SemanticCheckerBase {

    public NameModifierChecker (ClassInformationProvider cip, JavaTokens javaTokens,
				ParsedEntry tree, CompilerDiagnosticCollector diagnostics) {
	super (cip, javaTokens, tree, diagnostics);
    }

    @Override public void runCheck () {
	forAllTypes (this::check);
    }

    private void check (TypeDeclaration td) {
	boolean topLevel = td.getOuterClass () == null;
	checkNameAndFlags (td, topLevel);
	checkSuperclass (td);
	checkConstructors (td);
	checkFields (td, topLevel);
	checkMethods (td);
    }

    private void checkNameAndFlags (TypeDeclaration td, boolean topLevel) {
	int flags = td.flags ();
	int numFlags = 0;
	if (Flags.isPublic (flags)) {
	    String id = td.getName ();
	    if (topLevel && tree.getOrigin () != null &&
		!(id + ".java").equals (tree.getOrigin ().getFileName ().toString ()))
		error (td, "Public top level type: %s should be in a file named %s.java, not in %s",
		       id, id, tree.getOrigin ().getFileName ());
	    numFlags++;
	}
	if (Flags.isProtected (flags)) {
	    if (topLevel)
		error (td, "Top level type may not be protected");
	    numFlags++;
	}
	if (Flags.isPrivate (flags)) {
	    if (topLevel)
		error (td, "Top level type may not be private");
	    numFlags++;
	}

	if (numFlags > 1)
	    error (td, "Type has too many access flags");

	if (Flags.isStatic (flags) && topLevel)
	    error (td, "Top level type may not be static");
    }

    private void checkSuperclass (TypeDeclaration td) {
	ClassType superclass = td.getSuperClass ();
	if (superclass != null) {
	    int flags = cip.flags (superclass.fullName ());
	    if (Flags.isFinal (flags))
		error (superclass, "Can not extend final class");
	    if (Flags.isInterface (flags))
		error (superclass, "Can not extend interface");
	}
    }

    private void checkConstructors (TypeDeclaration td) {
	td.getConstructors ()
	    .forEach (cdi -> checkAccess (cdi.position (), cdi.flags ()));
    }

    private void checkFields (TypeDeclaration td, boolean topLevel) {
	Map<String, FieldInfo> fields = td.getFields ();
	fields.forEach ((name, fi) -> {
		int flags = fi.flags ();
		checkAccess (fi.pos (), flags);
		if (Flags.isFinal (flags) && Flags.isVolatile (flags))
		    error (fi.pos (), "Field may not be both final and volatile");
	    });
    }

    private void checkMethods (TypeDeclaration td) {
	td.getMethods ().forEach (this::checkMethod);
    }

    private void checkMethod (MethodDeclarationBase m) {
	int flags = m.flags ();
	checkAccess (m.position (), flags);
	if (Flags.isNative (flags) && Flags.isStrict (flags))
	    error (m, "method may not be both native and strictfp");

	if (Flags.isAbstract (flags)) {
	    if  (Flags.isPrivate (flags) || Flags.isStatic (flags) || Flags.isFinal (flags) ||
		 Flags.isNative (flags) || Flags.isStrict (flags) || Flags.isSynchronized (flags)) {
		error (m, "Mixing abstract with non allowed flags");
	    }
	}

	ParseTreeNode body = m.getMethodBody ();
	if (body instanceof Block) {
	    if (Flags.isAbstract (flags))
		error (m, "Abstract method may not have a body");
	    if (Flags.isNative (flags)) {
		error (m, "Native method may not have a body");
	    }
	} else {
	    if (!(Flags.isAbstract (flags) || Flags.isNative (flags))) {
		error (m, "Empty method body is only allowed for native and abstract methods");
	    }
	}
    }

    private void checkAccess (ParsePosition pos, int flags) {
	int count = 0;
	if (Flags.isPublic (flags))
	    count++;
	if (Flags.isProtected (flags))
	    count++;
	if (Flags.isPrivate (flags))
	    count++;
	if (count > 1)
	    error (pos, "Too many access flags, can only use one of: public, private andprotected)");
    }
}
