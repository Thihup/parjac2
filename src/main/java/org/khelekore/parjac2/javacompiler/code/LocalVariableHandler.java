package org.khelekore.parjac2.javacompiler.code;

import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.LocalVariableDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclarator;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.TypeKind;

public class LocalVariableHandler {

    public static void handleLocalVariables (MethodContentGenerator mcg, CodeBuilder cb, LocalVariableDeclaration lvs) {
	FullNameHandler fn = FullNameHelper.type (lvs.getType ());
	for (VariableDeclarator lv : lvs.getDeclarators ()) {
	    TypeKind kind = FullNameHelper.getTypeKind (fn);
	    int slot = cb.allocateLocal (kind);
	    lv.localSlot (slot);
	    ParseTreeNode init = lv.initializer ();
	    if (init != null) {
		mcg.handleStatements (cb, init);
		FullNameHandler fromType = FullNameHelper.type (init);
		CodeUtil.widenOrAutoBoxAsNeeded (cb, fromType, fn, kind);
		cb.storeLocal (kind, slot);
	    }
	}
    }
}
