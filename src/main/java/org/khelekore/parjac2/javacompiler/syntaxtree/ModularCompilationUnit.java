package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ModularCompilationUnit extends SyntaxTreeNode {
    private List<ImportDeclaration> imports;
    private ModuleDeclaration moduleDeclaration;

    public ModularCompilationUnit (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	imports = (rule.size () > 1) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	moduleDeclaration = (ModuleDeclaration)children.get (i++);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!imports.isEmpty ()) {
	    for (ImportDeclaration i : imports)
		sb.append (i).append ("\n");
	}
	sb.append (moduleDeclaration);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	imports.forEach (v::accept);
	v.accept (moduleDeclaration);
    }

    public ModuleDeclaration getModule () {
	return moduleDeclaration;
    }
}
