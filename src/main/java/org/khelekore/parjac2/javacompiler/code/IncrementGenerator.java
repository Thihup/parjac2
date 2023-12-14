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
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

public class IncrementGenerator {
    public static void handlePostIncrement (MethodContentGenerator mcg, CodeBuilder cb,
					    FullNameHandler currentClass, PostIncrementExpression pie) {
	handlePostChange (mcg, cb, currentClass, pie.expression (), 1, pie.valueIsUsed ());
    }

    public static void handlePostDecrement (MethodContentGenerator mcg, CodeBuilder cb,
					    FullNameHandler currentClass, PostDecrementExpression pde) {
	handlePostChange (mcg, cb, currentClass, pde.expression (), -1, pde.valueIsUsed ());
    }

    private static void handlePostChange (MethodContentGenerator mcg, CodeBuilder cb,
					  FullNameHandler currentClass, ParseTreeNode tn, int change, boolean valueIsUsed) {
	if (tn instanceof DottedName dn)
	    tn = dn.replaced ();
	if (tn instanceof FieldAccess fa) {
	    ParseTreeNode from = fa.from ();
	    VariableInfo vi = fa.variableInfo ();
	    switch (vi.fieldType ()) {
	    case VariableInfo.Type.PARAMETER -> incrementLocalVariable (cb, ((FormalParameterBase)vi).slot (), change, valueIsUsed);
	    case VariableInfo.Type.LOCAL -> incrementLocalVariable (cb, ((LocalVariable)vi).slot (), change, valueIsUsed);
	    case VariableInfo.Type.FIELD -> incrementField (mcg, cb, from, currentClass, vi, change, valueIsUsed);
	    case VariableInfo.Type.ARRAY_LENGTH -> throw new IllegalStateException ("Can not increment array.length");
	    }
	} else if (tn instanceof ArrayAccess aa) {
	    mcg.handleStatements (cb, aa.from ());
	    mcg.handleStatements (cb, aa.slot ());
	    cb.dup2 ();   // dup or dup2?
	    TypeKind kind = FullNameHelper.getTypeKind (FullNameHelper.type (aa));
	    cb.arrayLoadInstruction (kind);
	    /*
	    if (valueIsUsed)
		cb.dup ();
	    */
	    CodeUtil.handleInt (cb, change);
	    cb.iadd ();
	    cb.arrayStoreInstruction (kind);
	} else {
	    throw new IllegalStateException ("Unhandled post increment type: " + tn + ", " + tn.getClass ().getName () +
					     ", " + tn.position ().toShortString ());
	}
    }

    private static void incrementLocalVariable (CodeBuilder cb, int slot, int value, boolean valueIsUsed) {
	if (valueIsUsed)
	    cb.loadInstruction (TypeKind.IntType, slot);
	cb.incrementInstruction (slot, value);
    }

    private static void incrementField (MethodContentGenerator mcg, CodeBuilder cb,
					ParseTreeNode from, FullNameHandler currentClass, VariableInfo vi,
					int change, boolean valueIsUsed) {
	ClassDesc type = vi.typeClassDesc ();
	FieldGenerator.FromResult fr = FieldGenerator.handleFrom (mcg, cb, from, currentClass, vi);
	if (fr.instanceField ()) {
	    cb.dup ();
	    cb.getfield (fr.owner (), vi.name (), type);
	} else {
	    cb.getstatic (fr.owner (), vi.name (), type);
	}
	CodeUtil.handleInt (cb, change);
	cb.iadd ();
	if (fr.instanceField ())
	    cb.putfield (fr.owner (), vi.name (), type);
	else
	    cb.putstatic (fr.owner (), vi.name (), type);
    }
}
