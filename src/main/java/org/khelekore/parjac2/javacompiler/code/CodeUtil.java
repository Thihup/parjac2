package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.DoubleLiteral;
import org.khelekore.parjac2.javacompiler.IntLiteral;
import org.khelekore.parjac2.javacompiler.LongLiteral;
import org.khelekore.parjac2.javacompiler.syntaxtree.ConstructorDeclarationInfo;
import org.khelekore.parjac2.javacompiler.syntaxtree.FormalParameterBase;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

public class CodeUtil {

    public static void callSuperInit (CodeBuilder cb, TypeDeclaration td, ConstructorDeclarationInfo cdb) {
	cb.lineNumber (cdb.position ().getLineNumber ()); // about what we want
	cb.aload (0);
	ClassDesc owner = ClassDescUtils.getClassDesc (td.getSuperClass ());
	cb.invokespecial (owner, ConstantDescs.INIT_NAME, MethodTypeDesc.ofDescriptor ("()V"));
    }

    public static void loadParameter (CodeBuilder cb, FormalParameterBase fpb) {
	int slot = cb.parameterSlot (fpb.slot ());
	FullNameHandler type = fpb.typeName ();
	if (type == FullNameHandler.INT || type == FullNameHandler.BOOLEAN)
	    cb.iload (slot);
	else if (type == FullNameHandler.LONG)
	    cb.lload (slot);
	else if (type == FullNameHandler.DOUBLE)
	    cb.dload (slot);
	else if (type == FullNameHandler.FLOAT)
	    cb.fload (slot);
	else {  // TODO: more types
	    cb.aload (slot);
	}
    }

    public static void widenOrAutoBoxAsNeeded (CodeBuilder cb, FullNameHandler from, FullNameHandler to, TypeKind tkTo) {
	if (from.getType () == FullNameHandler.Type.PRIMITIVE) {
	    if (to.getType () == FullNameHandler.Type.OBJECT) {
		autoBox (cb, (FullNameHandler.Primitive)from);
	    } else if (!to.equals (from)) {
		TypeKind tkr = FullNameHelper.getTypeKind (from);
		cb.convertInstruction (tkr, tkTo);
	    }
	} else if (to.getType () == FullNameHandler.Type.PRIMITIVE) {
	    autoUnBox (cb, from, (FullNameHandler.Primitive)to);
	}
    }

    // We only know we want some kind of object, we can not pass in to to get the owner
    private static void autoBox (CodeBuilder cb, FullNameHandler.Primitive p) {
	FullNameHandler ab = FullNameHelper.getAutoBoxOption (p);
	ClassDesc owner = ClassDescUtils.getClassDesc (ab);
	String name = "valueOf";
	MethodTypeDesc type = MethodTypeDesc.ofDescriptor ("(" + p.getSignature () + ")L" + ab.getSlashName () + ";");
	cb.invokestatic (owner, name, type);
    }

    // from will be java.lang.Long and to will be long, so here we can trust them
    private static void autoUnBox (CodeBuilder cb, FullNameHandler from, FullNameHandler.Primitive to) {
	ClassDesc owner = ClassDescUtils.getClassDesc (from);
	String name = to.getFullDotName () + "Value"; // intValue, longValue, ...
	MethodTypeDesc type = MethodTypeDesc.ofDescriptor ("()"+ to.signature ());
	cb.invokevirtual (owner, name, type);
    }

    public static void widen (CodeBuilder cb, FullNameHandler targetType, ParseTreeNode p) {
	if (targetType == FullNameHandler.BOOLEAN)
	    targetType = FullNameHandler.INT;
	FullNameHandler pfn = FullNameHelper.type (p);
	if (pfn == FullNameHandler.BOOLEAN)
	    pfn = FullNameHandler.INT;
	if (pfn == targetType)
	    return;
	if (pfn.isPrimitive ()) {
	    TypeKind tkm = FullNameHelper.getTypeKind (targetType);
	    TypeKind tkr = FullNameHelper.getTypeKind (FullNameHelper.type (p));
	    addPrimitiveCast (cb, tkr, tkm);
	}
    }

    private static void addPrimitiveCast (CodeBuilder cb, TypeKind from, TypeKind to) {
	cb.convertInstruction (from, to);
    }

    public static void handleThis (CodeBuilder cb) {
	cb.aload (cb.receiverSlot ());
    }

    public static void handleInt (CodeBuilder cb, IntLiteral il) {
	int i = il.intValue ();
	handleInt (cb, i);
    }

    public static void handleInt (CodeBuilder cb, int i) {
	if (i >= -1 && i <= 5) {
	    switch (i) {
	    case -1 -> cb.iconst_m1 ();
	    case 0 -> cb.iconst_0 ();
	    case 1 -> cb.iconst_1 ();
	    case 2 -> cb.iconst_2 ();
	    case 3 -> cb.iconst_3 ();
	    case 4 -> cb.iconst_4 ();
	    case 5 -> cb.iconst_5 ();
	    }
	} else if (i >= -128 && i <= 127) {
	    cb.bipush (i);
	} else if (i >= -32768 && i <= 32767) {
	    cb.sipush (i);
	} else {
	    cb.ldc (i);
	}
    }

    public static void handleLong (CodeBuilder cb, LongLiteral l) {
	cb.ldc (l.longValue ());
    }

    public static void handleDouble (CodeBuilder cb, DoubleLiteral dl) {
	double d = dl.doubleValue ();
	if (d == 0) {
	    cb.dconst_0 ();
	} else if (d == 1) {
	    cb.dconst_1 ();
	} else {
	    cb.ldc (d);
	}
    }
}
