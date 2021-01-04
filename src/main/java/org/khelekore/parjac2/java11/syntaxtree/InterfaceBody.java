package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class InterfaceBody extends SyntaxTreeNode {
    private List<ParseTreeNode> declarations;

    private List<ParseTreeNode> constantDeclarations = new ArrayList<> ();
    private List<ParseTreeNode> interfaceMethodDeclarations = new ArrayList<> ();
    private List<TypeDeclaration> classDeclarations = new ArrayList<> ();

    public InterfaceBody (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 2) {
	    declarations = ((Multiple)children.get (1)).get ();
	} else {
	    declarations = Collections.emptyList ();
	}

	TypeDistributor td = DistributorHelper.getClassDistributor (classDeclarations);
	td.addMapping (ConstantDeclaration.class, constantDeclarations);
	td.addMapping (InterfaceMethodDeclaration.class, interfaceMethodDeclarations);
	declarations.forEach (td::distribute);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (" {\n");
	declarations.forEach (n -> sb.append (n).append ("\n"));
	sb.append ("}");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	declarations.forEach (v::accept);
    }

    public List<TypeDeclaration> getInnerClasses () {
	return classDeclarations;
    }
}
