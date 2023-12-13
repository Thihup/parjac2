package org.khelekore.parjac2.javacompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.AmbiguousName;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayCreationExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Assignment;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
import org.khelekore.parjac2.javacompiler.syntaxtree.CastExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassInstanceCreationExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassLiteral;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.CompactConstructorDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumConstant;
import org.khelekore.parjac2.javacompiler.syntaxtree.EnumDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.ExpressionName;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterList;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.LocalVariableDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.MethodInvocation;
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
	    r.addMethod (new MethodDeclaration (pos, flags, rc.name (), res, null,
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
    private static final int ENUM_VALUES_FLAGS = Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_FINAL | Flags.ACC_SYNTHETIC;
    private static final int ENUM_METHODS_FLAGS = Flags.ACC_PUBLIC | Flags.ACC_STATIC;
    private static final String VALUES_FIELD = "$VALUES";
    private static final int VALUES_METHOD_FLAGS = Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_SYNTHETIC;

    private void addEnumFieldsAndMethods (EnumDeclaration e) {
	// one field for each constant.
	for (EnumConstant ec : e.constants ()) {
	    ClassType ct = new ClassType (cip.getFullName (e));
	    e.addField (new FieldInfo (VariableInfo.Type.FIELD, ec.getName (), ec.position (), ENUM_FIELD_FLAGS, ct, 0));
	}

	// one field to hold the values
	ParsePosition pos = e.position ();
	ClassType ct = new ClassType (cip.getFullName (e));
	ArrayType at = new ArrayType (ct, 1);
	e.addField (new FieldInfo (VariableInfo.Type.FIELD, VALUES_FIELD, pos, ENUM_VALUES_FLAGS, at, 0));

	// E[] values()
	//     return (E[])$VALUES.clone();
	ParseTreeNode on = new FieldAccess (pos, null, VALUES_FIELD);
	ReturnStatement rs =
	    new ReturnStatement (pos, new CastExpression (at, new MethodInvocation (pos, on, "clone")));
	e.addMethod (new MethodDeclaration (pos, ENUM_METHODS_FLAGS, "values", at, null, new Block (pos, rs)));

	// E valueOf(String)
	// enum E { E valueOf (String e) { return Enum.valueOf (E.class, e); } ... }
	String VALUE_OF_FIELD = "e";
	on = new ClassType (FullNameHandler.JL_ENUM);
	ClassLiteral ctClass = new ClassLiteral (pos, ct, 0);
	ParseTreeNode[] args = { ctClass, new AmbiguousName (pos, List.of (VALUE_OF_FIELD)) };
	rs = new ReturnStatement (pos, new CastExpression (ct, new MethodInvocation (pos, on, "valueOf", args)));
	FormalParameterList params = getParams (pos, FullNameHandler.JL_STRING, VALUE_OF_FIELD);
	e.addMethod (new MethodDeclaration (pos, ENUM_METHODS_FLAGS, "valueOf", ct, params, new Block (pos, rs)));

	// private constructor
	List<ConstructorDeclaration> ls = e.getConstructors ();
	if (ls.isEmpty ()) {
	    int flags = Flags.ACC_PRIVATE;
	    List<ParseTreeNode> superCallArguments = List.of (new AmbiguousName (pos, List.of ("name")),
							      new AmbiguousName (pos, List.of ("ordinal")));
	    List<FormalParameterBase> constructorParams =
		List.of (getParam (pos, FullNameHandler.JL_STRING, "name"),
			 getParam (pos, FullNameHandler.INT, "ordinal"));
	    List<ParseTreeNode> statements = new ArrayList<> ();
	    ls.add (ConstructorDeclaration.create (pos, javaTokens, flags, e.getName (),
						   superCallArguments, constructorParams, statements));
	}

	// private static E[] $values();
	// E[] es = new E[2];
	// es[0] = Y;
	// es[1] = N;
	// return es
	String localArray = "es";
	List<ParseTreeNode> statements = new ArrayList<> ();
	ParseTreeNode array = new AmbiguousName (pos, List.of (localArray));
	IntLiteral il = new IntLiteral (javaTokens.INT_LITERAL, e.constants ().size (), pos);
	ParseTreeNode init = new ArrayCreationExpression (pos, ct, il);
	statements.add (new LocalVariableDeclaration (pos, at, localArray, init));
	List<EnumConstant> constants = e.constants ();
	for (int i = 0; i < constants.size (); i++) {
	    statements.add (getArrayAssignment (pos, array, constants.get (i), i));
	}
	statements.add (new ReturnStatement (pos, array));
	e.addMethod (new MethodDeclaration (pos, VALUES_METHOD_FLAGS, "$values", at, null, new Block (pos, statements)));

	// static {};
	// Y = new E ("Y", 0); N = new E ("N", 1); $VALUES = $values ();
	statements = new ArrayList<> ();
	for (int i = 0; i < constants.size (); i++) {
	    String name = constants.get (i).getName ();
	    statements.add (getEnumInstanceCreation (pos, ct, name, i));
	}
	statements.add (new Assignment (new ExpressionName (pos, VALUES_FIELD),
					javaTokens.EQUAL,
					new MethodInvocation (pos, null, "$values")));
	Block block = new Block (pos, statements);
	e.addStaticInitializer (block);
    }

    private Assignment getArrayAssignment (ParsePosition pos, ParseTreeNode array, EnumConstant ec, int slot) {
	String name = ec.getName ();
	ParseTreeNode left = new ArrayAccess (pos, array, intLiteral (pos, slot));
	ParseTreeNode right = new ExpressionName (pos, name);
	return new Assignment (left, javaTokens.EQUAL, right);
    }

    private FormalParameterList getParams (ParsePosition pos, FullNameHandler type, String name) {
	return new FormalParameterList (pos, List.of (getParam (pos, type, name)));
    }

    private FormalParameter getParam (ParsePosition pos, FullNameHandler type, String name) {
	ClassType ct = new ClassType (type);
	return new FormalParameter (pos, List.of (), ct, name, 0);
    }

    private Assignment getEnumInstanceCreation (ParsePosition pos, ClassType type, String name, int ordinal) {
	ParseTreeNode left = new ExpressionName (pos, name);
	ParseTreeNode nameNode = new StringLiteral (javaTokens.STRING_LITERAL, name, pos);
	ParseTreeNode right = new ClassInstanceCreationExpression (pos, type, nameNode, intLiteral (pos, ordinal));
	return new Assignment (left, javaTokens.EQUAL, right);
    }

    private IntLiteral intLiteral (ParsePosition pos, int value) {
	return new IntLiteral (javaTokens.INT_LITERAL, value, pos);
    }

    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.position (), template, args));
    }
}
