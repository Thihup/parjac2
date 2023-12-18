package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.LocalVariable;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.VariableInfo;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.DottedName;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.PostDecrementExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.PostIncrementExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.PreDecrementExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.PreIncrementExpression;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

public class IncrementGenerator {

    public static void handlePostIncrement (MethodContentGenerator mcg, CodeBuilder cb,
					    FullNameHandler currentClass, PostIncrementExpression pie) {
	handlePostChange (mcg, cb, currentClass, pie.expression (), 1, pie.valueIsUsed (), true);
    }

    public static void handlePostDecrement (MethodContentGenerator mcg, CodeBuilder cb,
					    FullNameHandler currentClass, PostDecrementExpression pde) {
	handlePostChange (mcg, cb, currentClass, pde.expression (), -1, pde.valueIsUsed (), true);
    }

    public static void handlePreIncrement (MethodContentGenerator mcg, CodeBuilder cb,
					   FullNameHandler currentClass, PreIncrementExpression pie) {
	handlePostChange (mcg, cb, currentClass, pie.expression (), 1, pie.valueIsUsed (), false);
    }

    public static void handlePreDecrement (MethodContentGenerator mcg, CodeBuilder cb,
					   FullNameHandler currentClass, PreDecrementExpression pde) {
	handlePostChange (mcg, cb, currentClass, pde.expression (), -1, pde.valueIsUsed (), false);
    }

    private static void handlePostChange (MethodContentGenerator mcg, CodeBuilder cb,
					  FullNameHandler currentClass, ParseTreeNode tn, int change,
					  boolean valueIsUsed, boolean valueFromBeforeChange) {
	if (tn instanceof DottedName dn)
	    tn = dn.replaced ();
	if (tn instanceof FieldAccess fa) {
	    ParseTreeNode from = fa.from ();
	    VariableInfo vi = fa.variableInfo ();
	    FullNameHandler fn = FullNameHelper.type (fa);
	    TypeKind kind = FullNameHelper.getTypeKind (fn);
	    switch (vi.fieldType ()) {
	    case VariableInfo.Type.PARAMETER -> incrementLocalVariable (cb, kind, ((FormalParameterBase)vi).slot (), change, valueIsUsed, valueFromBeforeChange);
	    case VariableInfo.Type.LOCAL -> incrementLocalVariable (cb, kind, ((LocalVariable)vi).slot (), change, valueIsUsed, valueFromBeforeChange);
	    case VariableInfo.Type.FIELD -> incrementField (mcg, cb, kind, from, currentClass, vi, change, valueIsUsed, valueFromBeforeChange);
	    case VariableInfo.Type.ARRAY_LENGTH -> throw new IllegalStateException ("Can not increment array.length");
	    }
	} else if (tn instanceof ArrayAccess aa) {
	    mcg.handleStatements (cb, aa.from ());
	    mcg.handleStatements (cb, aa.slot ());
	    cb.dup2 ();   // dup or dup2?
	    TypeKind kind = FullNameHelper.getTypeKind (FullNameHelper.type (aa));
	    cb.arrayLoadInstruction (kind);
	    if (valueIsUsed && valueFromBeforeChange)
		arrayDupX2 (cb, kind);
	    addOne (cb, kind, change);
	    if (valueIsUsed && !valueFromBeforeChange)
		arrayDupX2 (cb, kind);
	    cb.arrayStoreInstruction (kind);
	} else {
	    throw new IllegalStateException ("Unhandled post increment type: " + tn + ", " + tn.getClass ().getName () +
					     ", " + tn.position ().toShortString ());
	}
    }

    private static void arrayDupX2 (CodeBuilder cb, TypeKind kind) {
	if (kind == TypeKind.DoubleType || kind == TypeKind.LongType)
	    cb.dup2_x2 ();
	else
	    cb.dup_x2 ();
    }

    private static void incrementLocalVariable (CodeBuilder cb, TypeKind kind, int slot, int value,
						boolean valueIsUsed, boolean valueFromBeforeChange) {
	switch (kind) {
	case IntType -> incInt (cb, slot, value, valueIsUsed, valueFromBeforeChange);
	case DoubleType -> incDouble (cb, slot, value, valueIsUsed, valueFromBeforeChange);
	case FloatType -> incFloat (cb, slot, value, valueIsUsed, valueFromBeforeChange);
	default -> incIntegral (cb, kind, slot, value, valueIsUsed, valueFromBeforeChange);
	}
    }

    private static void incInt (CodeBuilder cb, int slot, int value, boolean valueIsUsed, boolean valueFromBeforeChange) {
	if (valueIsUsed && valueFromBeforeChange)
	    cb.iload (slot);
	cb.incrementInstruction (slot, value);
	if (valueIsUsed && !valueFromBeforeChange)
	    cb.iload (slot);
    }

    private static void incDouble (CodeBuilder cb, int slot, int value, boolean valueIsUsed, boolean valueFromBeforeChange) {
	cb.dload (slot);
	if (valueIsUsed && valueFromBeforeChange)
	    cb.dup2 ();
	cb.dconst_1 ();
	if (value == 1)
	    cb.dadd ();
	else
	    cb.dsub ();
	if (valueIsUsed && !valueFromBeforeChange)
	    cb.dup2 ();
	cb.dstore (slot);
    }

    private static void incFloat (CodeBuilder cb, int slot, int value, boolean valueIsUsed, boolean valueFromBeforeChange) {
	cb.fload (slot);
	if (valueIsUsed && valueFromBeforeChange)
	    cb.dup ();
	cb.fconst_1 ();
	if (value == 1)
	    cb.fadd ();
	else
	    cb.fsub ();
	if (valueIsUsed && !valueFromBeforeChange)
	    cb.dup ();
	cb.fstore (slot);
    }

    private static void incIntegral (CodeBuilder cb, TypeKind tk, int slot, int value, boolean valueIsUsed, boolean valueFromBeforeChange) {
	cb.iload (slot);
	if (valueIsUsed && valueFromBeforeChange)
	    cb.dup ();
	CodeUtil.handleInt (cb, value);
	cb.iadd ();
	if (valueIsUsed && !valueFromBeforeChange)
	    cb.dup ();
	cb.convertInstruction(TypeKind.IntType, tk);
	cb.istore (slot);
    }

    private static void incrementField (MethodContentGenerator mcg, CodeBuilder cb, TypeKind kind,
					ParseTreeNode from, FullNameHandler currentClass, VariableInfo vi,
					int value, boolean valueIsUsed, boolean valueFromBeforeChange) {
	ClassDesc type = vi.typeClassDesc ();
	FieldGenerator.FromResult fr = FieldGenerator.handleFrom (mcg, cb, from, currentClass, vi);
	if (fr.instanceField ()) {
	    cb.dup ();
	    cb.getfield (fr.owner (), vi.name (), type);
	} else {
	    cb.getstatic (fr.owner (), vi.name (), type);
	}
	if (valueIsUsed && valueFromBeforeChange)
	    dup (cb, kind, fr.instanceField ());
	addOne (cb, kind, value);
	if (valueIsUsed && !valueFromBeforeChange)
	    dup (cb, kind, fr.instanceField ());
	if (fr.instanceField ())
	    cb.putfield (fr.owner (), vi.name (), type);
	else
	    cb.putstatic (fr.owner (), vi.name (), type);
    }

    private static void dup (CodeBuilder cb, TypeKind kind, boolean instanceField) {
	if (kind == TypeKind.DoubleType || kind == TypeKind.LongType) {
	    if (instanceField)
		cb.dup2_x1 ();
	    else
		cb.dup2 ();
	} else {
	    if (instanceField)
		cb.dup_x1 ();
	    else
		cb.dup ();
	}
    }

    private static void addOne (CodeBuilder cb, TypeKind kind, int value) {
	switch (kind) {
	case IntType -> { CodeUtil.handleInt (cb, value); cb.iadd (); }
	case DoubleType -> incDoubleField (cb, value);
	case FloatType -> incFloatField (cb, value);
	default -> incIntegralField (cb, value);
	}
    }

    private static void incDoubleField (CodeBuilder cb, int value) {
	cb.dconst_1 ();
	if (value == 1)
	    cb.dadd ();
	else
	    cb.dsub ();
    }

    private static void incFloatField (CodeBuilder cb, int value) {
	cb.fconst_1 ();
	if (value == 1)
	    cb.fadd ();
	else
	    cb.fsub ();
    }

    private static void incIntegralField (CodeBuilder cb, int value) {
	cb.iconst_1 ();
	if (value == 1)
	    cb.iadd ();
	else
	    cb.isub ();
    }
}
