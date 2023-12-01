package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayCreationExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

public class ArrayGenerator {
    public static void handleArrayCreation (MethodContentGenerator mcg, CodeBuilder cb, ArrayCreationExpression ace) {
	mcg.handleStatements (cb, ace.getChildren ());
	FullNameHandler type = ace.innerFullName ();
	if (ace.rank () == 1) {
	    if (type.isPrimitive ()) {
		TypeKind kind = FullNameHelper.getTypeKind (type);
		cb.newarray (kind);
	    } else {
		ClassDesc desc = ClassDescUtils.getClassDesc (type);
		cb.anewarray (desc);
	    }
	} else {
	    ClassDesc desc = ClassDescUtils.getClassDesc (ace.fullName ());
	    cb.multianewarray (desc, ace.dimExprs ());
	}
    }

    public static void handleArrayAccess (MethodContentGenerator mcg, CodeBuilder cb, ArrayAccess aa) {
	mcg.handleStatements (cb, aa.from ());
	mcg.handleStatements (cb, aa.slot ());
	TypeKind tk = FullNameHelper.getTypeKind (FullNameHelper.type (aa));
	cb.arrayLoadInstruction (tk);
    }

}
