package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.VariableInfo;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;

public class FieldGenerator {

    public static void getField (MethodContentGenerator mcg, CodeBuilder cb,
				 ParseTreeNode from, FullNameHandler currentClass, VariableInfo vi) {
	FromResult fr = handleFrom (mcg, cb, from, currentClass, vi);
	ClassDesc type = vi.typeClassDesc ();
	if (fr.instanceField ()) {
	    cb.getfield (fr.owner (), vi.name (), type);
	} else {
	    cb.getstatic (fr.owner (), vi.name (), type);
	}
    }

    public static void putField (MethodContentGenerator mcg, FullNameHandler fieldOwner,
				 CodeBuilder cb, VariableInfo vi, ParseTreeNode value) {
	// TODO: handle "from" better
	ClassDesc owner = ClassDescUtils.getClassDesc (fieldOwner);
	ClassDesc type = vi.typeClassDesc ();
	if (Flags.isInstanceField (vi))
	    cb.aload (cb.receiverSlot ());
	mcg.handleStatements (cb, value);
	if (Flags.isInstanceField (vi)) {
	    cb.putfield (owner, vi.name (), type);
	} else {
	    cb.putstatic (owner, vi.name (), type);
	}
    }

    public static FromResult handleFrom (MethodContentGenerator mcg, CodeBuilder cb,
					ParseTreeNode from, FullNameHandler currentClass, VariableInfo vi) {
	ClassDesc owner;
	boolean instanceField = Flags.isInstanceField (vi);
	if (from instanceof ClassType ct) {
	    owner = ClassDescUtils.getClassDesc (ct);
	    instanceField = false;
	} else if (from != null) {
	    mcg.handleStatements (cb, from);
	    owner = ClassDescUtils.getClassDesc (FullNameHelper.type (from));
	} else {
	    if (instanceField)
		cb.aload (cb.receiverSlot ());
	    owner = ClassDescUtils.getClassDesc (currentClass);
	}
	return new FromResult (owner, instanceField);
    }

    public record FromResult (ClassDesc owner, boolean instanceField) { /* empty */ }
}
