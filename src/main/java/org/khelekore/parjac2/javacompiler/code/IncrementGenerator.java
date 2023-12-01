package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.ClassInformationProvider;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.LocalVariable;
import org.khelekore.parjac2.javacompiler.VariableInfo;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.DottedName;
import org.khelekore.parjac2.javacompiler.syntaxtree.FieldAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.PostDecrementExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.PostIncrementExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;

public class IncrementGenerator {
    public static void handlePostIncrement (ClassInformationProvider cip, TypeDeclaration td,
					    CodeBuilder cb, PostIncrementExpression pie) {
	handlePostChange (cip, td, cb, pie.expression (), 1);
    }

    public static void handlePostDecrement (ClassInformationProvider cip, TypeDeclaration td,
					    CodeBuilder cb, PostDecrementExpression pde) {
	handlePostChange (cip, td, cb, pde.expression (), -1);
    }

    private static void handlePostChange (ClassInformationProvider cip, TypeDeclaration td,
					  CodeBuilder cb, ParseTreeNode tn, int change) {
	if (tn instanceof DottedName dn)
	    tn = dn.replaced ();
	if (tn instanceof FieldAccess fa) {
	    ParseTreeNode from = fa.from ();
	    if (from != null) {
		// TODO: implement
	    } else {
		VariableInfo vi = fa.variableInfo ();
		switch (vi.fieldType ()) {
		case VariableInfo.Type.PARAMETER -> incrementLocalVariable (cb, ((FormalParameterBase)vi).slot (), change);
		case VariableInfo.Type.LOCAL -> incrementLocalVariable (cb, ((LocalVariable)vi).slot (), change);
		case VariableInfo.Type.FIELD -> incrementField (cip, td, cb, vi, change);
		}
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

    private static void incrementField (ClassInformationProvider cip, TypeDeclaration td,
					CodeBuilder cb, VariableInfo vi, int change) {
	ClassDesc owner = ClassDescUtils.getClassDesc (cip.getFullName (td));
	ClassDesc type = vi.typeClassDesc ();
	if (Flags.isInstanceField (vi)) {
	    cb.aload (cb.receiverSlot ());
	    cb.dup ();
	}
	if (Flags.isInstanceField (vi))
	    cb.getfield (owner, vi.name (), type);
	else
	    cb.getstatic (owner, vi.name (), type);
	CodeUtil.handleInt (cb, change);
	cb.iadd ();
	if (Flags.isInstanceField (vi))
	    cb.putfield (owner, vi.name (), type);
	else
	    cb.putstatic (owner, vi.name (), type);
    }
}
