package org.khelekore.parjac2.javacompiler;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.javacompiler.syntaxtree.OrdinaryCompilationUnit;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class SemanticCheckerBase {
    protected final ClassInformationProvider cip;
    protected final JavaTokens javaTokens;
    protected final ParsedEntry tree;
    protected final CompilerDiagnosticCollector diagnostics;

    public static <T extends SemanticCheckerBase>
	void runChecks (ClassInformationProvider cip,
			JavaTokens javaTokens,
			List<ParsedEntry> trees,
			CompilerDiagnosticCollector diagnostics,
			SemanticCheckerBaseFactory<T> factory) {
	List<T> checkers =
	    trees.stream ()
	    .filter (pe -> (pe.getRoot () instanceof OrdinaryCompilationUnit))
	    .map (t -> factory.create (cip, javaTokens, t, diagnostics)).collect (Collectors.toList ());
	checkers.parallelStream ().forEach (SemanticCheckerBase::runCheck);
    }

    public SemanticCheckerBase (ClassInformationProvider cip, JavaTokens javaTokens,
				ParsedEntry tree, CompilerDiagnosticCollector diagnostics) {
	this.cip = cip;
	this.javaTokens = javaTokens;
	this.tree = tree;
	this.diagnostics = diagnostics;
    }

    protected abstract void runCheck ();

    protected void forAllTypes (Consumer<TypeDeclaration> tc) {
	TypeTraverser.forAllTypes ((OrdinaryCompilationUnit)tree.getRoot (), tc);
    }

    protected void error (ParseTreeNode where, String template, Object... args) {
	error (where.position (), template, args);
    }

    protected void error (ParsePosition where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.error (tree.getOrigin (), where, template, args));
    }

    protected void warning (ParseTreeNode where, String template, Object... args) {
	warning (where.position (), template, args);
    }

    protected void warning (ParsePosition where, String template, Object... args) {
	diagnostics.report (SourceDiagnostics.warning (tree.getOrigin (), where, template, args));
    }

    public interface SemanticCheckerBaseFactory<T extends SemanticCheckerBase> {
	T create (ClassInformationProvider cip, JavaTokens javaTokens,
		  ParsedEntry tree, CompilerDiagnosticCollector diagnostics);
    }
}
