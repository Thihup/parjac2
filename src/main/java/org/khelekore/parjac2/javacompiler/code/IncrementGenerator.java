package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.LocalVariable;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.VariableInfo;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.DottedName;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.PostDecrementExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.PostIncrementExpression;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;

public class IncrementGenerator {
    public static void handlePostIncrement (MethodContentGenerator mcg, CodeBuilder cb,
					    FullNameHandler currentClass, PostIncrementExpression pie) {
	handlePostChange (mcg, cb, currentClass, pie.expression (), 1);
    }

    public static void handlePostDecrement (MethodContentGenerator mcg, CodeBuilder cb,
					    FullNameHandler currentClass, PostDecrementExpression pde) {
	handlePostChange (mcg, cb, currentClass, pde.expression (), -1);
    }

    private static void handlePostChange (MethodContentGenerator mcg, CodeBuilder cb,
					  FullNameHandler currentClass, ParseTreeNode tn, int change) {
	if (tn instanceof DottedName dn)
	    tn = dn.replaced ();
	if (tn instanceof FieldAccess fa) {
	    ParseTreeNode from = fa.from ();
	    VariableInfo vi = fa.variableInfo ();
	    switch (vi.fieldType ()) {
	    case VariableInfo.Type.PARAMETER -> incrementLocalVariable (cb, ((FormalParameterBase)vi).slot (), change);
	    case VariableInfo.Type.LOCAL -> incrementLocalVariable (cb, ((LocalVariable)vi).slot (), change);
	    case VariableInfo.Type.FIELD -> incrementField (mcg, cb, from, currentClass, vi, change);
	    }
	} else if (tn instanceof ArrayAccess aa) {
	    // TODO: implement
	} else {
	    throw new IllegalStateException ("Unhandled post increment type: " + tn + ", " + tn.getClass ().getName () +
					     ", " + tn.position ().toShortString ());
	}
    }

    private static void incrementLocalVariable (CodeBuilder cb, int slot, int value) {
	cb.incrementInstruction (slot, value);
    }

    private static void incrementField (MethodContentGenerator mcg, CodeBuilder cb,
					ParseTreeNode from, FullNameHandler currentClass, VariableInfo vi, int change) {
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
