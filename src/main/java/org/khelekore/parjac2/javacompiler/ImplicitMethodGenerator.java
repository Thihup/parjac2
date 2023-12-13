package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.code.BytecodeBlockBase;
import org.khelekore.parjac2.javacompiler.code.CodeUtil;
import org.khelekore.parjac2.javacompiler.syntaxtree.AmbiguousName;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayType;
import org.khelekore.parjac2.javacompiler.syntaxtree.Assignment;
import org.khelekore.parjac2.javacompiler.syntaxtree.Block;
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

import io.github.dmlloyd.classfile.CodeBuilder;

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
    private static final String VALUES_METHOD_NAME = "$values";
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
	e.addMethod (new MethodDeclaration (pos, ENUM_METHODS_FLAGS, "values", at, null,
					    new Block (pos, new EnumCodeBlock (e, this::values))));

	// E valueOf(String)
	FormalParameterList params = getParams (pos, FullNameHandler.JL_STRING, "$e");
	e.addMethod (new MethodDeclaration (pos, ENUM_METHODS_FLAGS, "valueOf", ct, params,
					    new Block (pos, new EnumCodeBlock (e, this::valueOf))));

	// private constructor
	List<ConstructorDeclaration> ls = e.getConstructors ();
	if (ls.isEmpty ()) {
	    int flags = Flags.ACC_PRIVATE;
	    List<ParseTreeNode> superCallArguments = List.of (new AmbiguousName (pos, List.of ("name")),
							      new AmbiguousName (pos, List.of ("ordinal")));
	    List<FormalParameterBase> constructorParams =
		List.of (getParam (pos, FullNameHandler.JL_STRING, "name"),
			 getParam (pos, FullNameHandler.INT, "ordinal"));
	    List<ParseTreeNode> statements = List.of ();
	    ls.add (ConstructorDeclaration.create (pos, javaTokens, flags, e.getName (),
						   superCallArguments, constructorParams, statements));
	}

	// private static E[] $values();
	e.addMethod (new MethodDeclaration (pos, VALUES_METHOD_FLAGS, VALUES_METHOD_NAME, at, null,
					    new Block (pos, new EnumCodeBlock (e, this::$values))));

	// static {};
	e.addStaticInitializer (new Block (pos, new EnumCodeBlock (e, this::enumStaticBlock)));
    }

    private FormalParameterList getParams (ParsePosition pos, FullNameHandler type, String name) {
	return new FormalParameterList (pos, List.of (getParam (pos, type, name)));
    }

    private FormalParameter getParam (ParsePosition pos, FullNameHandler type, String name) {
	ClassType ct = new ClassType (type);
	return new FormalParameter (pos, List.of (), ct, name, 0);
    }

    private static final MethodTypeDesc CLONE_SIGNATURE = MethodTypeDesc.of (ConstantDescs.CD_Object);
    // basically it is;
    // return (E[])$VALUES.clone();
    private void values (CodeBuilder cb, EnumDeclaration ed) {
	ClassDesc owner = ClassDescUtils.getClassDesc (cip.getFullName (ed));
	ClassDesc arraytype = owner.arrayType ();
	cb.getstatic (owner, VALUES_FIELD, arraytype);
	cb.invokevirtual (arraytype, "clone", CLONE_SIGNATURE);
	cb.checkcast (arraytype);
    }

    private static final MethodTypeDesc VALUE_OF_SIGNATURE =
	MethodTypeDesc.of (ConstantDescs.CD_Enum, ConstantDescs.CD_Class, ConstantDescs.CD_String);

    // return Enum.valueOf (E.class, e); }
    private void valueOf (CodeBuilder cb, EnumDeclaration ed) {
	ClassDesc ourEnum = ClassDescUtils.getClassDesc (cip.getFullName (ed));
	cb.ldc (ourEnum);
	cb.aload (0);
	cb.invokestatic (ConstantDescs.CD_Enum, "valueOf", VALUE_OF_SIGNATURE);
	cb.checkcast (ourEnum);
    }

    // private static E[] $values();
    // E[] es = new E[2];
    // es[0] = Y;
    // es[1] = N;
    // return es
    private void $values (CodeBuilder cb, EnumDeclaration ed) {
	List<EnumConstant> constants = ed.constants ();
	CodeUtil.handleInt (cb, constants.size ());
	ClassDesc ourEnum = ClassDescUtils.getClassDesc (cip.getFullName (ed));
	cb.anewarray (ourEnum);
	for (int i = 0; i < constants.size (); i++) {
	    cb.dup ();
	    CodeUtil.handleInt (cb, i);
	    EnumConstant ec = constants.get (i);
	    cb.getstatic (ourEnum, ec.getName (), ourEnum);
	    cb.aastore ();
	}
    }

    private static final MethodTypeDesc ENUM_INIT_SIGNATURE =
	MethodTypeDesc.of (CodeUtil.CD_Void, ConstantDescs.CD_String, ConstantDescs.CD_int);

    // Y = new E ("Y", 0); N = new E ("N", 1); $VALUES = $values ();
    private void enumStaticBlock (CodeBuilder cb, EnumDeclaration ed) {
	ClassDesc ourEnum = ClassDescUtils.getClassDesc (cip.getFullName (ed));
	List<EnumConstant> constants = ed.constants ();
	for (int i = 0; i < constants.size (); i++) {
	    cb.new_ (ourEnum);
	    cb.dup ();
	    String name = constants.get (i).getName ();
	    cb.ldc (name);
	    CodeUtil.handleInt (cb, i);
	    cb.invokespecial (ourEnum, ConstantDescs.INIT_NAME, ENUM_INIT_SIGNATURE);
	    cb.putstatic (ourEnum, name, ourEnum);
	}

	ClassDesc arrayType = ourEnum.arrayType ();
	MethodTypeDesc mtd = MethodTypeDesc.of (arrayType);
	cb.invokestatic (ourEnum, VALUES_METHOD_NAME, mtd);
	cb.putstatic (ourEnum, VALUES_FIELD, arrayType);
    }


    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.position (), template, args));
    }

    private class EnumCodeBlock extends BytecodeBlockBase {
	private final EnumDeclaration ed;
	private final EnumCodeGenerator ecb;

	public EnumCodeBlock (EnumDeclaration ed, EnumCodeGenerator ecb) {
	    super (ed.position ());
	    this.ed = ed;
	    this.ecb = ecb;
	}

	@Override public void generate (CodeBuilder cb) {
	    ecb.generate (cb, ed);
	}
    }

    private interface EnumCodeGenerator {
	void generate (CodeBuilder cb, EnumDeclaration ed);
    }
}
