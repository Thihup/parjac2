package org.khelekore.parjac2.javacompiler;

import java.lang.constant.ClassDesc;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.ParsePosition;

public class DuplicateFinder {
    private final ClassInformationProvider cip;
    private final ParsedEntry tree;
    private final CompilerDiagnosticCollector diagnostics;
    private final OrdinaryCompilationUnit ocu; // we do not care about ModularCompilationUnit (for now?)

    public static void findDuplicateMethods (ClassInformationProvider cip,
					     List<ParsedEntry> trees,
					     CompilerDiagnosticCollector diagnostics) {
	List<DuplicateFinder> finders =
	    trees.stream ()
	    .filter (pe -> (pe.getRoot () instanceof OrdinaryCompilationUnit))
	    .map (t -> new DuplicateFinder (cip, t, diagnostics)).collect (Collectors.toList ());
	finders.parallelStream ().forEach (DuplicateFinder::findDuplicateMethods);
    }

    public DuplicateFinder (ClassInformationProvider cip,
			    ParsedEntry pe,
			    CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.tree = pe;
	this.diagnostics = diagnostics;
	ocu = (OrdinaryCompilationUnit)pe.getRoot ();
    }

    public void findDuplicateMethods () {
	TypeTraverser.forAllTypes (ocu, this::findDuplicateMethods);
    }

    public void findDuplicateMethods (TypeDeclaration td) {
	FullNameHandler fqn = cip.getFullName (td);
	Map<String, List<MethodInfo>> methods = td.getMethodInformation (fqn);
	methods.values ().forEach (this::findDuplicateMethods);
    }

    public void findDuplicateMethods (List<MethodInfo> ls) {
	Set<List<ClassDesc>> seen = new HashSet<> ();
	for (MethodInfo mi : ls) {
	    List<ClassDesc> params = mi.methodTypeDesc ().parameterList ();
	    if (!seen.add (params)) {
		error (mi.position (), "Duplicate method found");
	    }
	}
    }

    private void error (ParsePosition where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where, template, args));
    }
}

