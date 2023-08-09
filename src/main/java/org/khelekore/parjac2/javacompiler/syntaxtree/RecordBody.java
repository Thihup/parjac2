package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class RecordBody extends ClassBody {

    protected List<CompactConstructorDeclaration> compactConstructors;

    public RecordBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (ctx, rule, n, children);
    }

    @Override protected void addAdditionalMappings (TypeDistributor td) {
	// have to create it here
	compactConstructors = new ArrayList<> ();
	td.addMapping (CompactConstructorDeclaration.class, compactConstructors);
    }
}
