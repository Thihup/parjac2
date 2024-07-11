package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayAccess;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayCreationExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.ArrayInitializer;
import org.khelekore.parjac2.javacompiler.syntaxtree.DimExprs;
import org.khelekore.parjac2.javacompiler.syntaxtree.Dims;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

public class ArrayGenerator {
    public static void handleArrayCreation (MethodContentGenerator mcg, CodeBuilder cb, ArrayCreationExpression ace) {
	// since all children of ace includes the type we call handleStatements on the parts we need.
	DimExprs dimExprs = ace.dimExprs ();
	if (dimExprs != null)
	    mcg.handleStatements (cb, dimExprs);
	Dims dims = ace.dims ();
	if (dims != null)
	    mcg.handleStatements (cb, dims);
	FullNameHandler type = ace.innerFullName ();
	if (ace.rank () == 1) {
	    if (type.isPrimitive ()) {
		TypeKind kind = FullNameHelper.getTypeKind (type);
		cb.newarray (kind);
	    } else {
		ClassDesc desc = ClassDescUtils.getClassDesc (type);
		cb.anewarray (desc);

		ArrayInitializer init = ace.initializer ();
		if (init != null) {
		    TypeKind kind = FullNameHelper.getTypeKind (ace.innerFullName ());
		    setSlotData (mcg, cb, init, kind);
		}
	    }
	} else {
	    ClassDesc desc = ClassDescUtils.getClassDesc (ace.fullName ());
	    cb.multianewarray (desc, ace.dimExprsRank ());
	}
    }

    public static void handleArrayAccess (MethodContentGenerator mcg, CodeBuilder cb, ArrayAccess aa) {
	mcg.handleStatements (cb, aa.from ());
	mcg.handleStatements (cb, aa.slot ());
	TypeKind tk = FullNameHelper.getTypeKind (FullNameHelper.type (aa));
	cb.arrayLoad (tk);
    }

    public static void handleArrayInitializer (MethodContentGenerator mcg, CodeBuilder cb, ArrayInitializer ai) {
	int numSlots = ai.size ();
	CodeUtil.handleInt (cb, numSlots);
	FullNameHandler type = ai.slotType ();
	TypeKind kind = FullNameHelper.getTypeKind (ai.slotType ());
	if (type.isPrimitive ()) {
	    cb.newarray (kind);
	} else {
	    ClassDesc cd = ClassDescUtils.getClassDesc (type);
	    cb.anewarray (cd);
	}

	setSlotData (mcg, cb, ai, kind);
    }

    private static void setSlotData (MethodContentGenerator mcg, CodeBuilder cb, ArrayInitializer ai, TypeKind kind) {
	int pos = 0;
	for (ParseTreeNode p : ai.variableInitializers ()) {
	    cb.dup ();
	    CodeUtil.handleInt (cb, pos++);
	    mcg.handleStatements (cb, p);
	    cb.arrayStore (kind);
	}
    }
}
