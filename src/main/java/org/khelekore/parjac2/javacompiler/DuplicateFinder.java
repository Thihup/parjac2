package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.ParsePosition;

import io.github.dmlloyd.classfile.attribute.ExceptionsAttribute;
import io.github.dmlloyd.classfile.constantpool.ClassEntry;

public class DuplicateFinder extends SemanticCheckerBase {

    public DuplicateFinder (ClassInformationProvider cip, JavaTokens javaTokens,
			    ParsedEntry pe, CompilerDiagnosticCollector diagnostics) {
	super (cip, javaTokens, pe, diagnostics);
    }

    @Override public void runCheck () {
	forAllTypes (this::findDuplicateMethods);
    }

    public void findDuplicateMethods (TypeDeclaration td) {
	FullNameHandler fqn = cip.getFullName (td);
	Map<String, List<MethodInfo>> methods = td.getMethodInformation (fqn);
	methods.values ().forEach (this::findDuplicateMethods);
	methods.values ().stream ().flatMap (Collection::stream).forEach (this::checkThrows);
    }

    public void findDuplicateMethods (List<MethodInfo> ls) {
	Map<List<ClassDesc>, MethodInfo> seen = new HashMap<> ();
	for (MethodInfo mi : ls) {
	    List<ClassDesc> params = mi.methodTypeDesc ().parameterList ();
	    MethodInfo previous = seen.put (params, mi);
	    if (previous != null) {
		ParsePosition mip = mi.position ();
		ParsePosition pp = previous.position ();
		ParsePosition last = ParsePosition.PositionComparator.compare (mip, pp) <= 0 ? pp : mip;
		error (last, "Duplicate method found");
	    }
	}
    }

    public void checkThrows (MethodInfo mi) {
	ExceptionsAttribute ea = mi.exceptions ();
	if (ea != null) {
	    List<ClassEntry> ts = ea.exceptions ();
	    Set<ClassDesc> seen = new HashSet<> ();
	    for (ClassEntry ce : ts) {
		ClassDesc cd = ce.asSymbol ();
		if (!seen.add (cd))
		    warning (mi.position (), "Duplicate exception thrown: %s", cd.displayName ());
	    }
	}
    }
}

