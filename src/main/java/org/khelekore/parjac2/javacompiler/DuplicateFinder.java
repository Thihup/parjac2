package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.ClassType;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.ParsePosition;

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
	List<ClassType> ts = mi.thrownTypes ();
	if (ts != null) {
	    Set<String> seen = new HashSet<> ();
	    for (ClassType ct : ts) {
		String fqn = ct.getFullDotName ();
		if (!seen.add (fqn))
		    warning (ct, "Duplicate exception thrown: %s", fqn);
	    }
	}
    }
}

