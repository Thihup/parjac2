package org.khelekore.parjac2.javacompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.Assignment;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.CompactConstructorDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclarationInfo;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumConstant;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExpressionName;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.NormalClassDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.RecordComponent;
import org.khelekore.parjac2.javacompiler.syntaxtree.RecordDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ReturnStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.ThisPrimary;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ImplicitMethodGenerator {

    private final ClassInformationProvider cip;
    private final JavaTokens javaTokens;
    private final ParsedEntry tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final OrdinaryCompilationUnit ocu; // we do not care about ModularCompilationUnit (for now?)

    /** Add implicit constructors, fields and methods to the given classes
     */
    public static void addImplicitMethods (ClassInformationProvider cip,
					   JavaTokens javaTokens,
					   List<ParsedEntry> trees,
					   CompilerDiagnosticCollector diagnostics) {
	List<ImplicitMethodGenerator> imgs =
	    trees.stream ()
	    .filter (pe -> (pe.getRoot () instanceof OrdinaryCompilationUnit))
	    .map (t -> new ImplicitMethodGenerator (cip, javaTokens, t, diagnostics)).collect (Collectors.toList ());
	if (diagnostics.hasError ())
	    return;
	imgs.parallelStream ().forEach (ImplicitMethodGenerator::addMethods);
    }

    public ImplicitMethodGenerator (ClassInformationProvider cip,
				    JavaTokens javaTokens,
				    ParsedEntry pe,
				    CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.javaTokens = javaTokens;
	tree = pe;
	this.diagnostics = diagnostics;
	ocu = (OrdinaryCompilationUnit)pe.getRoot ();
    }

    private void addMethods () {
	TypeTraverser.forAllTypes (ocu, this::addImplicits);
    }

    private void addImplicits (TypeDeclaration td) {
	switch (td) {
	case NormalClassDeclaration c -> addDefaultConstructor (c);
	case RecordDeclaration r -> addRecordFieldsAndMethods (r);
	case EnumDeclaration e -> addEnumFieldsAndMethods (e);
	default -> { /* empty */ }
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
	    ls.add (ConstructorDeclaration.create (n.position (), javaTokens, flags, id, List.of (), List.of (), List.of ()));
	}
    }

    private static final int RECORD_FIELD_FLAGS = Flags.ACC_PRIVATE | Flags.ACC_FINAL;

    private void addRecordFieldsAndMethods (RecordDeclaration r) {
	List<RecordComponent> rcs = r.getRecordComponents ();
	for (RecordComponent rc : rcs) {
	    r.addField (new FieldInfo (VariableInfo.Type.FIELD, rc.name (), rc.position (), RECORD_FIELD_FLAGS, rc.type (), 0));
	}

	// TODO: we need to handle compact constructor inlining.
	List<CompactConstructorDeclaration> ccds = r.getCompactConstructors ();
	if (ccds.size () > 1)
	    error (ccds.get (1), "Can not have more than one compact constructor");
	CompactConstructorDeclaration ccd = ccds.size () > 0 ? ccds.get (0) : null;

	List<ConstructorDeclaration> ls = r.getConstructors ();
	if (ls.isEmpty () || ccd != null) {
	    int flags = Flags.isPublic (r.flags ()) ? Flags.ACC_PUBLIC : 0;
	    List<FormalParameterBase> params = new ArrayList<> ();
	    List<ParseTreeNode> statements = new ArrayList<> ();
	    if (ccd != null) {
		statements.addAll (ccd.statements ());
	    }

	    for (RecordComponent rc : rcs) {
		params.add (getFormalParameter (rc));
		statements.add (getAssignment (rc));
	    }
	    ParsePosition position = ccd != null ? ccd.position () : r.position ();
	    ls.add (ConstructorDeclaration.create (position, javaTokens, flags, r.getName (), List.of (), params, statements));
	}

	// We do not add toString, equals or hashCode here, they are added in bytecode generation
	// Since they exist in Object we will find them if we use them anyway.

	// Add a field-getter for each field.
	for (RecordComponent rc : rcs) {
	    int flags = Flags.ACC_PUBLIC;
	    ParsePosition pos = rc.position ();
	    ParseTreeNode res = rc.type ();
	    r.addMethod (new MethodDeclaration (pos, flags, rc.name (), res,
						new Block (pos, new ReturnStatement (pos, rc.name ()))));
	}
    }

    private FormalParameterBase getFormalParameter (RecordComponent rc) {
	return new FormalParameter (rc.position (), List.of (), rc.type (), rc.name (), 0);
    }

    private ParseTreeNode getAssignment (RecordComponent rc) {
	ParsePosition pos = rc.position ();
	ParseTreeNode left = new FieldAccess (pos, new ThisPrimary (rc), rc.name ());
	ParseTreeNode right = new ExpressionName (pos, rc.name ());
	return new Assignment (left, javaTokens.EQUAL, right);
    }

    private static final int ENUM_FIELD_FLAGS = Flags.ACC_PUBLIC | Flags.ACC_STATIC | Flags.ACC_FINAL | Flags.ACC_ENUM;

    private void addEnumFieldsAndMethods (EnumDeclaration e) {
	// TODO: implement
	// fields
	for (EnumConstant ec : e.constants ()) {
	    ClassType ct = new ClassType (cip.getFullName (e));
	    e.addField (new FieldInfo (VariableInfo.Type.FIELD, ec.getName (), ec.position (), ENUM_FIELD_FLAGS, ct, 0));
	}
	// E[] values()
	// E valueOf(String)
	// private constructor
	if (e.getConstructors ().isEmpty ()) {
	    // Add a default one
	}
	// private static E[] $values();
	// static {};
    }

    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.position (), template, args));
    }
}
