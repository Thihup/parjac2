package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ModularCompilationUnit extends SyntaxTreeNode {
    private List<ImportDeclaration> imports;
    private ModuleDeclaration moduleDeclaration;

    public ModularCompilationUnit (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
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
}
