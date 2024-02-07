package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.util.List;

import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;

import io.github.dmlloyd.classfile.attribute.ExceptionsAttribute;

public class ExceptionsAttributeUtils {
    public static ExceptionsAttribute get (List<ClassType> ls) {
	if (ls == null)
	    return null;
	List<ClassDesc> cds = ls.stream ().map (ClassDescUtils::getClassDesc).toList ();
	return ExceptionsAttribute.ofSymbols (cds);
    }
}
