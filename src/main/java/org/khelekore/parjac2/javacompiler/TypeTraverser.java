package org.khelekore.parjac2.javacompiler;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;

public class TypeTraverser {
    public static void forAllTypes (OrdinaryCompilationUnit ocu,
				    Consumer<TypeDeclaration> handler) {
	Deque<TypeDeclaration> typesToHandle = new ArrayDeque<> ();
	typesToHandle.addAll (ocu.getTypes ());
	while (!typesToHandle.isEmpty ()) {
	    TypeDeclaration td = typesToHandle.removeFirst ();
	    handler.accept (td);
	    typesToHandle.addAll (td.getInnerClasses ());
	}
    }
}
