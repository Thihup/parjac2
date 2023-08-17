package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class InterfaceBody extends SyntaxTreeNode {
    private List<ParseTreeNode> declarations; // all of the things found in the interface

    // Change this, currently FieldDeclaration have VariableDeclaratorList in them, we want a list of fields.
    private List<ConstantDeclaration> constantDeclarations = new ArrayList<> ();
    private List<InterfaceMethodDeclaration> interfaceMethodDeclarations = new ArrayList<> ();

    // inner classes, enums, interfaces and annotations
    private List<TypeDeclaration> classDeclarations = new ArrayList<> ();

    public InterfaceBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
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

	for (TypeDeclaration i : classDeclarations) {
	    int flags = i.getFlags ();
	    int clash = flags & (Flags.ACC_PRIVATE | Flags.ACC_PROTECTED);
	    if (clash > 0) {
		ctx.error (i.getPosition (), "Interface member type may not be %s",
			   ctx.getTokenNameString (clash));
	    }
	    flags |= Flags.ACC_PUBLIC | Flags.ACC_STATIC;
	    i.setFlags (flags);
	}
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

    public List<ConstantDeclaration> getConstants () {
	return constantDeclarations;
    }

    public List<InterfaceMethodDeclaration> getMethods () {
	return interfaceMethodDeclarations;
    }
}
