package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

import org.khelekore.parjac2.javacompiler.ClassInformationProvider;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;

import io.github.dmlloyd.classfile.ClassBuilder;
import io.github.dmlloyd.classfile.attribute.InnerClassInfo;
import io.github.dmlloyd.classfile.attribute.InnerClassesAttribute;

public class AttributeHelper {

    /* We need to add all nested classes, not just the direct inner classes of tdt.
     * Here is how it may look for: class Inners { class Inner1 { class Inner1_Inner1 {} } class Inner2 }
     #21= #14 of #7;   // Inner2=class Inners$Inner2 of class Inners
     #22= #16 of #7;   // Inner1=class Inners$Inner1 of class Inners
     #23= #18 of #16;  // Inner1_Inner1=class Inners$Inner1$Inner1_Inner1 of class Inners$Inner1
    */
    public static void addInnerClassAttributes (ClassBuilder classBuilder, ClassInformationProvider cip, TypeDeclaration td) {
	List<InnerClassInfo> innerClassInfos = new ArrayList<> ();
	Deque<TypeDeclaration> queue = new ArrayDeque<> ();
	queue.add (td);
	while (!queue.isEmpty ()) {
	    TypeDeclaration outer = queue.removeFirst ();
	    for (TypeDeclaration inner : outer.getInnerClasses ()) {
		innerClassInfos.add (getInnerClassInfo (outer, cip, inner));
		queue.add (inner);
	    }
	}
	/* We need to add outer classes as well.
	 * For the Inner1_Inner1 above javac produces:
	 #20= #18 of #15;   // Inner1=class Inners$Inner1 of class Inners
	 #21= #7 of #18;    // Inner1_Inner1=class Inners$Inner1$Inner1_Inner1 of class Inners$Inner1
	*/
	TypeDeclaration outer = td.getOuterClass ();
	TypeDeclaration inner = td;
	while (outer != null) {
	    innerClassInfos.add (getInnerClassInfo (outer, cip, inner));
	    inner = outer;
	    outer = outer.getOuterClass ();
	}

	if (!innerClassInfos.isEmpty ())
	    classBuilder.with (InnerClassesAttribute.of (innerClassInfos));
    }

    private static InnerClassInfo getInnerClassInfo (TypeDeclaration outer, ClassInformationProvider cip, TypeDeclaration inner) {
	ClassDesc innerClass = ClassDesc.of (cip.getFullName (inner).getFullDollarName ());
	Optional<ClassDesc> outerClass = Optional.of (ClassDesc.of (cip.getFullName (outer).getFullDollarName ()));
	Optional<String> innerName = Optional.of (inner.getName ());
	int flags = inner.flags ();
	return InnerClassInfo.of (innerClass, outerClass, innerName, flags);
    }
}
