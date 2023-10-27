package org.khelekore.parjac2.javacompiler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalClassDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ImplicitMethodGenerator {

    private final JavaTokens javaTokens;
    private final ParsedEntry tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final OrdinaryCompilationUnit ocu; // we do not care about ModularCompilationUnit (for now?)

    /** High level description:
     *  For each class:
     *      Setup a Scope
     *      Find fields, set type on them, add to scope of class
     *      Find methods, set type on arguments and add to scope of method
     *      Set type of expression
     */
    public static void addImplicitMethods (JavaTokens javaTokens,
					   List<ParsedEntry> trees,
					   CompilerDiagnosticCollector diagnostics) {
	List<ImplicitMethodGenerator> imgs =
	    trees.stream ()
	    .filter (pe -> (pe.getRoot () instanceof OrdinaryCompilationUnit))
	    .map (t -> new ImplicitMethodGenerator (javaTokens, t, diagnostics)).collect (Collectors.toList ());
	if (diagnostics.hasError ())
	    return;
	imgs.parallelStream ().forEach (ImplicitMethodGenerator::addMethods);
    }

    public ImplicitMethodGenerator (JavaTokens javaTokens,
				    ParsedEntry pe,
				    CompilerDiagnosticCollector diagnostics) {
	this.javaTokens = javaTokens;
	tree = pe;
	this.diagnostics = diagnostics;
	ocu = (OrdinaryCompilationUnit)pe.getRoot ();
    }

    private void addMethods () {
	forAllTypes (this::addDefaultConstructor);
    }

    private void addDefaultConstructor (TypeDeclaration td) {
	if (td instanceof NormalClassDeclaration c)
	    addDefaultConstructor (c);
	// TODO: handle more types
    }

    private void addDefaultConstructor (NormalClassDeclaration n) {
	String id = n.getName ();
	List<ConstructorDeclaration> ls = n.getConstructors ();
	for (ConstructorDeclaration cd : ls) {
	    if (!id.equals (cd.getName ()))
		error (cd, "Constructor name: %s, does not match class name: %s", cd.getName (), id);
	}
	if (ls.isEmpty ()) {
	    int flags = Flags.isPublic (n.flags ()) ? Flags.ACC_PUBLIC : 0;
	    ls.add (ConstructorDeclaration.create (n.getPosition (), javaTokens, flags, id, List.of ()));
	}
    }

    private void forAllTypes (Consumer<TypeDeclaration> handler) {
	Deque<TypeDeclaration> typesToHandle = new ArrayDeque<> ();
	typesToHandle.addAll (ocu.getTypes ());
	while (!typesToHandle.isEmpty ()) {
	    TypeDeclaration td = typesToHandle.removeFirst ();
	    handler.accept (td);
	    typesToHandle.addAll (td.getInnerClasses ());
	}
    }

    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.getPosition (), template, args));
    }
}
