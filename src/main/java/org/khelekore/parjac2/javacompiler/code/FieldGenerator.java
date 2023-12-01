package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.ClassInformationProvider;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.VariableInfo;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;

public class FieldGenerator {

    public static void getField (ClassInformationProvider cip, TypeDeclaration td, CodeBuilder cb, VariableInfo vi) {
	ClassDesc owner = ClassDescUtils.getClassDesc (cip.getFullName (td));
	ClassDesc type = vi.typeClassDesc ();
	if (Flags.isInstanceField (vi)) {
	    cb.aload (cb.receiverSlot ());
	    cb.getfield (owner, vi.name (), type);
	} else {
	    cb.getstatic (owner, vi.name (), type);
	}
    }

    public static void putField (MethodContentGenerator mcg, FullNameHandler fieldOwner,
				 CodeBuilder cb, VariableInfo vi, ParseTreeNode value) {
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
}
