package org.khelekore.parjac2.javacompiler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalClassDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.RecordComponent;
import org.khelekore.parjac2.javacompiler.syntaxtree.RecordDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ReturnStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.ParsePosition;
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
	forAllTypes (this::addImplicits);
    }

    private void addImplicits (TypeDeclaration td) {
	switch (td) {
	case NormalClassDeclaration c -> addDefaultConstructor (c);
	case RecordDeclaration r -> addRecordFieldsAndMethods (r);
	case EnumDeclaration e -> addEnumFieldsAndMethods (e);
	default -> {}
	}
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

    private static final int RECORD_FIELD_FLAGS = Flags.ACC_PRIVATE | Flags.ACC_FINAL;

    private void addRecordFieldsAndMethods (RecordDeclaration r) {
	List<RecordComponent> rcs = r.getRecordComponents ();
	for (RecordComponent rc : rcs) {
	    r.addField (new FieldInfo (rc.name (), rc.getPosition (), RECORD_FIELD_FLAGS, rc.type (), 0));
	}
	// Add: Constructor, toString(), hashCode(), equals(Object o)

	// Add a field-getter for each field.
	for (RecordComponent rc : rcs) {
	    int flags = Flags.ACC_PUBLIC;
	    ParsePosition pos = rc.getPosition ();
	    ParseTreeNode res = rc.type ();
	    r.addMethod (new MethodDeclaration (pos, flags, rc.name (), res,
						new Block (pos, new ReturnStatement (pos, rc.name ()))));
	}
    }

    private void addEnumFieldsAndMethods (EnumDeclaration e) {
	// TODO: implement
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
