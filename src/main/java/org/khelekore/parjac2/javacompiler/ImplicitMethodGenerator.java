package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.code.BytecodeBlockBase;
import org.khelekore.parjac2.javacompiler.code.CodeUtil;
import org.khelekore.parjac2.javacompiler.code.DynamicGenerator;
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

    private static final int RECORD_METHOD_FLAGS = Flags.ACC_PUBLIC | Flags.ACC_FINAL;

    private static final int ENUM_FIELD_FLAGS = Flags.ACC_PUBLIC | Flags.ACC_STATIC | Flags.ACC_FINAL | Flags.ACC_ENUM;
    private static final int ENUM_VALUES_FLAGS = Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_FINAL | Flags.ACC_SYNTHETIC;
    private static final int ENUM_METHODS_FLAGS = Flags.ACC_PUBLIC | Flags.ACC_STATIC;
    private static final String VALUES_FIELD = "$VALUES";
    private static final String VALUES_METHOD_NAME = "$values";
    private static final int VALUES_METHOD_FLAGS = Flags.ACC_PRIVATE | Flags.ACC_STATIC | Flags.ACC_SYNTHETIC;

    private static final MethodTypeDesc CLONE_SIGNATURE = MethodTypeDesc.of (ConstantDescs.CD_Object);
    private static final MethodTypeDesc VALUE_OF_SIGNATURE =
	MethodTypeDesc.of (ConstantDescs.CD_Enum, ConstantDescs.CD_Class, ConstantDescs.CD_String);
    private static final MethodTypeDesc ENUM_INIT_SIGNATURE =
	MethodTypeDesc.of (ConstantDescs.CD_void, ConstantDescs.CD_String, ConstantDescs.CD_int);

    public ImplicitMethodGenerator (ClassInformationProvider cip,
				    JavaTokens javaTokens,
				    ParsedEntry pe,
				    CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.javaTokens = javaTokens;
	tree = pe;
	this.diagnostics = diagnostics;
    }

    private static final int RECORD_FIELD_FLAGS = Flags.ACC_PRIVATE | Flags.ACC_FINAL;

    public void addImplicitFields (TypeDeclaration td) {
	switch (td) {
	case RecordDeclaration r -> addRecordFields (r);
	case EnumDeclaration e -> addEnumFields (e);
	default -> { /* empty */ }
	}
    }

    public List<MethodDeclaration> addImplicitMethods (TypeDeclaration td) {
	return switch (td) {
	case RecordDeclaration r -> addRecordMethods (r);
	case EnumDeclaration e -> addEnumMethods (e);
	default -> List.of ();
	};
    }

    public List<ConstructorDeclaration> addImplicitConstructors (TypeDeclaration td) {
	ConstructorDeclaration c = switch (td) {
	case NormalClassDeclaration n -> addDefaultConstructor (n);
	case RecordDeclaration r -> addRecordConstructor (r);
	case EnumDeclaration e -> addEnumConstructor (e);
	default -> null;
	};
	return c == null ? List.of () : List.of (c);
    }

    public void addImplicitStaticBlocks (TypeDeclaration td) {
	switch (td) {
	case EnumDeclaration e -> addEnumStaticBlock (e);
	default -> { /* empty */ }
	}
    }

    private void addRecordFields (RecordDeclaration r) {
	for (RecordComponent rc : r.getRecordComponents ()) {
	    r.addField (new FieldInfo (VariableInfo.Type.FIELD, rc.name (), rc.position (), RECORD_FIELD_FLAGS, rc.type (), 0));
	}
    }

    private void addEnumFields (EnumDeclaration e) {
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
    }

    private List<MethodDeclaration> addRecordMethods (RecordDeclaration r) {
	List<MethodDeclaration> ret = new ArrayList<> ();

	// toString, equals or hashCode
	ParsePosition rpos = r.position ();
	if (!hasMethod (r, "toString"))
	    addImplicit (rpos, r, FullNameHandler.JL_STRING, "toString", null, this::recordToString, ret);

	if (!hasMethod (r, "hashCode"))
	    addImplicit (rpos, r, FullNameHandler.INT, "hashCode", null, this::recordHashCode, ret);

	if (!hasMethod (r, "equals", FullNameHandler.JL_OBJECT)) {
	    FormalParameterList params = getParams (rpos, FullNameHandler.JL_OBJECT, "$o");
	    addImplicit (rpos, r, FullNameHandler.BOOLEAN, "equals", params, this::recordEquals, ret);
	}

	// Add a field-getter for each field.
	for (RecordComponent rc : r.getRecordComponents ()) {
	    int flags = Flags.ACC_PUBLIC;
	    ParsePosition pos = rc.position ();
	    ParseTreeNode res = rc.type ();
	    MethodDeclaration m = new MethodDeclaration (pos, flags, rc.name (), res, null,
							 new Block (pos, new ReturnStatement (pos, rc.name ())));
	    r.addMethod (m);
	    ret.add (m);
	}
	return ret;
    }

    private boolean hasMethod (RecordDeclaration rd, String name, FullNameHandler... argTypes) {
	FullNameHandler fqn = cip.getFullName (rd);
	List<MethodInfo> ls = rd.getMethodInformation (fqn, name);
	for (MethodInfo mi : ls) {
	    if (mi.numberOfArguments () == argTypes.length && allTypesMatch (mi, argTypes)) {
		return true;
	    }
	}
	return false;
    }

    private boolean allTypesMatch (MethodInfo mi, FullNameHandler... argTypes) {
	for (int i = 0; i < mi.numberOfArguments (); i++)
	    if (!mi.parameter (i).equals (argTypes[i]))
		return false;
	return true;
    }

    private void addImplicit (ParsePosition rpos, RecordDeclaration r,
			      FullNameHandler returnType, String methodName, FormalParameterList params,
			      Generator<RecordDeclaration> generator, List<MethodDeclaration> ls) {
	ClassType ctRet = new ClassType (returnType);
	MethodDeclaration m = new MethodDeclaration (rpos, RECORD_METHOD_FLAGS, methodName, ctRet, params,
						     new Block (rpos, new RecordCodeBlock (r, generator)));
	r.addMethod (m);
	ls.add (m);
    }

    private void recordToString (CodeBuilder cb, RecordDeclaration rd) {
	DynamicGenerator.callObjectMethods (cb, rd, cip.getFullName (rd), "toString", FullNameHandler.JL_STRING);
	cb.areturn ();
    }

    private void recordHashCode (CodeBuilder cb, RecordDeclaration rd) {
	DynamicGenerator.callObjectMethods (cb, rd, cip.getFullName (rd), "hashCode", FullNameHandler.INT);
	cb.ireturn ();
    }

    private void recordEquals (CodeBuilder cb, RecordDeclaration rd) {
	DynamicGenerator.callObjectMethods (cb, rd, cip.getFullName (rd), "equals", FullNameHandler.BOOLEAN, FullNameHandler.JL_OBJECT);
	cb.ireturn ();
    }

    private ConstructorDeclaration addDefaultConstructor (NormalClassDeclaration n) {
	String id = n.getName ();
	List<ConstructorDeclaration> ls = n.getConstructors ();
	for (ConstructorDeclaration cd : ls) {
	    if (!id.equals (cd.getName ()))
		error (cd, "Constructor name: %s, does not match class name: %s", cd.getName (), id);
	}
	if (ls.isEmpty ()) {
	    int flags = Flags.isPublic (n.flags ()) ? Flags.ACC_PUBLIC : 0;
	    ConstructorDeclaration cd =
		ConstructorDeclaration.create (n.position (), javaTokens, flags, id, List.of (), List.of (), List.of ());
	    n.addConstructor (cd);
	    return cd;
	}
	return null;
    }

    private ConstructorDeclaration addRecordConstructor (RecordDeclaration r) {
	List<CompactConstructorDeclaration> ccds = r.getCompactConstructors ();
	if (ccds.size () > 1)
	    error (ccds.get (1), "Can not have more than one compact constructor");
	CompactConstructorDeclaration ccd = ccds.size () > 0 ? ccds.get (0) : null;

	ConstructorDeclaration c = null;
	List<ConstructorDeclaration> ls = r.getConstructors ();
	if (ls.isEmpty () || ccd != null) {
	    int flags = Flags.isPublic (r.flags ()) ? Flags.ACC_PUBLIC : 0;
	    List<FormalParameterBase> params = new ArrayList<> ();
	    List<ParseTreeNode> statements = new ArrayList<> ();
	    if (ccd != null) {
		statements.addAll (ccd.statements ());
	    }

	    for (RecordComponent rc : r.getRecordComponents ()) {
		params.add (getFormalParameter (rc));
		statements.add (getAssignment (rc));
	    }
	    ParsePosition position = ccd != null ? ccd.position () : r.position ();
	    c = ConstructorDeclaration.create (position, javaTokens, flags, r.getName (), List.of (), params, statements);
	    r.addConstructor (c);
	}
	return c;
    }

    private ConstructorDeclaration addEnumConstructor (EnumDeclaration e) {
	ParsePosition pos = e.position ();
	// private constructor
	List<ConstructorDeclaration> ls = e.getConstructors ();
	ConstructorDeclaration c = null;
	if (ls.isEmpty ()) {
	    int flags = Flags.ACC_PRIVATE;
	    List<ParseTreeNode> superCallArguments = List.of (new AmbiguousName (pos, List.of ("name")),
							      new AmbiguousName (pos, List.of ("ordinal")));
	    List<FormalParameterBase> constructorParams =
		List.of (getParam (pos, FullNameHandler.JL_STRING, "name"),
			 getParam (pos, FullNameHandler.INT, "ordinal"));
	    List<ParseTreeNode> statements = List.of ();
	    c = ConstructorDeclaration.create (pos, javaTokens, flags, e.getName (),
					       superCallArguments, constructorParams, statements);
	    e.addConstructor (c);
	}
	return c;
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

    private class RecordCodeBlock extends GenericCodeBlock<RecordDeclaration> {
	public RecordCodeBlock (RecordDeclaration rd, Generator<RecordDeclaration> g) {
	    super (rd, g);
	}
    }

    private List<MethodDeclaration> addEnumMethods (EnumDeclaration e) {
	List<MethodDeclaration> ret = new ArrayList<> ();

	ParsePosition pos = e.position ();
	ClassType ct = new ClassType (cip.getFullName (e));
	ArrayType at = new ArrayType (ct, 1);
	// E[] values()
	addMethod (e, new MethodDeclaration (pos, ENUM_METHODS_FLAGS, "values", at, null,
					     new Block (pos, new EnumCodeBlock (e, this::values))), ret);
	// E valueOf(String)
	FormalParameterList params = getParams (pos, FullNameHandler.JL_STRING, "$e");
	addMethod (e, new MethodDeclaration (pos, ENUM_METHODS_FLAGS, "valueOf", ct, params,
					     new Block (pos, new EnumCodeBlock (e, this::valueOf))), ret);
	// private static E[] $values();
	addMethod (e, new MethodDeclaration (pos, VALUES_METHOD_FLAGS, VALUES_METHOD_NAME, at, null,
					     new Block (pos, new EnumCodeBlock (e, this::$values))), ret);
	return ret;
    }

    private void addMethod (EnumDeclaration e, MethodDeclaration m, List<MethodDeclaration> ls) {
	e.addMethod (m);
	ls.add (m);
    }

    private FormalParameterList getParams (ParsePosition pos, FullNameHandler type, String name) {
	return new FormalParameterList (pos, List.of (getParam (pos, type, name)));
    }

    private FormalParameter getParam (ParsePosition pos, FullNameHandler type, String name) {
	ClassType ct = new ClassType (type);
	return new FormalParameter (pos, List.of (), ct, name, 0);
    }

    // basically it is;
    // return (E[])$VALUES.clone();
    private void values (CodeBuilder cb, EnumDeclaration ed) {
	ClassDesc owner = ClassDescUtils.getClassDesc (cip.getFullName (ed));
	ClassDesc arraytype = owner.arrayType ();
	cb.getstatic (owner, VALUES_FIELD, arraytype);
	cb.invokevirtual (arraytype, "clone", CLONE_SIGNATURE);
	cb.checkcast (arraytype);
	cb.areturn ();
    }

    // return Enum.valueOf (E.class, e); }
    private void valueOf (CodeBuilder cb, EnumDeclaration ed) {
	ClassDesc ourEnum = ClassDescUtils.getClassDesc (cip.getFullName (ed));
	cb.ldc (ourEnum);
	cb.aload (0);
	cb.invokestatic (ConstantDescs.CD_Enum, "valueOf", VALUE_OF_SIGNATURE);
	cb.checkcast (ourEnum);
	cb.areturn ();
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
	cb.areturn ();
    }

    private void addEnumStaticBlock (EnumDeclaration e) {
	// static {};
	e.addStaticInitializer (new Block (e.position (), new EnumCodeBlock (e, this::enumStaticBlock)));
    }

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
	cb.return_ ();
    }

    private void error (ParseTreeNode where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where.position (), template, args));
    }

    private class EnumCodeBlock extends GenericCodeBlock<EnumDeclaration> {
	public EnumCodeBlock (EnumDeclaration ed, Generator<EnumDeclaration> g) {
	    super (ed, g);
	}
    }

    private class GenericCodeBlock<T extends ParseTreeNode> extends BytecodeBlockBase {
	private final T t;
	private final Generator<T> tg;

	public GenericCodeBlock (T t, Generator<T> tg) {
	    super (t.position ());
	    this.t = t;
	    this.tg = tg;
	}

	@Override public void generate (CodeBuilder cb) {
	    tg.generate (cb, t);
	}
    }

    private interface Generator<T> {
	void generate (CodeBuilder cb, T t);
    }
}
