package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class OrdinaryCompilationUnit extends SyntaxTreeNode {
    private PackageDeclaration packageDeclarataion;
    private List<ImportDeclaration> imports;
    private List<TypeDeclaration> types;

    public OrdinaryCompilationUnit (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (rule.size () > i && rule.get (i) == ctx.getGrammar ().getRuleGroupId ("PackageDeclaration"))
	    packageDeclarataion = (PackageDeclaration)children.get (i++);
	else
	    packageDeclarataion = null;

	if (rule.size () > i && isZomImports (ctx.getGrammar (), children.get (i)))
	    imports = ((Multiple)children.get (i++)).<ImportDeclaration>get ();
	else
	    imports = Collections.emptyList ();

	if (rule.size () > i)
	    types = ((Multiple)children.get (i++)).<TypeDeclaration>get ();
	else
	    types = Collections.emptyList ();
    }

    private boolean isZomImports (Grammar grammar, ParseTreeNode n) {
	if (n instanceof Multiple) {
	    Multiple z = (Multiple)n;
	    if (z.getInternalGroupId () == grammar.getRuleGroupId ("ImportDeclaration"))
		return true;
	}
	return false;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (packageDeclarataion != null)
	    sb.append (packageDeclarataion).append ("\n");
	for (ImportDeclaration id : imports)
	    sb.append (id).append ("\n");
	for (TypeDeclaration type : types)
	    sb.append (type).append ("\n");
	return sb.toString ();
    }
}
