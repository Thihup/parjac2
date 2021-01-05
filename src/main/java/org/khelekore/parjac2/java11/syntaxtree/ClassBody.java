package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class ClassBody extends SyntaxTreeNode {
    protected List<ParseTreeNode> declarations; // all of them

    protected List<ParseTreeNode> instanceInitializers = new ArrayList<> ();
    protected List<ParseTreeNode> staticInitializers = new ArrayList<> ();
    protected List<ParseTreeNode> constructorDeclarations = new ArrayList<> ();
    protected List<ParseTreeNode> fieldDeclarations = new ArrayList<> ();
    protected List<ParseTreeNode> methodDeclarations = new ArrayList<> ();

    // inner classes, enums, interfaces and annotations
    protected List<TypeDeclaration> classDeclarations = new ArrayList<> ();

    public ClassBody (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	declarations = hasDeclarations (rule) ? ((Multiple)children.get (1)).get () : Collections.emptyList ();
	TypeDistributor td = DistributorHelper.getClassDistributor (classDeclarations);
	td.addMapping (Block.class, instanceInitializers);
	td.addMapping (StaticInitializer.class, staticInitializers);
	td.addMapping (ConstructorDeclaration.class, constructorDeclarations);
	td.addMapping (FieldDeclaration.class, fieldDeclarations);
	td.addMapping (MethodDeclaration.class, methodDeclarations);
	declarations.forEach (td::distribute);

	declarations.forEach (this::findInnerClasses);
    }

    private void findInnerClasses (ParseTreeNode n) {
	// We do a depth first search for classes.
	int foundClassId = 0;
	Deque<ParseTreeNode> dq = new ArrayDeque<> ();
	dq.addFirst (n);
	while (!dq.isEmpty ()) {
	    ParseTreeNode f = dq.removeFirst ();
	    if (isAnonymousClass (f)) {
		AnonymousClass ac = (AnonymousClass)f;
		ac.setAnonymousClassname (Integer.toString (++foundClassId));
		classDeclarations.add (ac);
	    } else {
		int end = dq.size ();
		// since we we want to add all the child nodes first in deque we first add them last
		// and them move them first so that things end up in wanted order
		f.visitChildNodes (cn -> dq.addLast (cn));
		int diff = dq.size () - end; // we added this many child nodes
		for (int i = 0; i < diff; i++)
		    dq.addFirst (dq.removeLast ());
	    }
	}
    }

    private boolean isAnonymousClass (ParseTreeNode f) {
	if (f instanceof UnqualifiedClassInstanceCreationExpression) {
	    UnqualifiedClassInstanceCreationExpression u = (UnqualifiedClassInstanceCreationExpression)f;
	    return u.hasBody ();
	} else if (f instanceof EnumConstant) {
	    EnumConstant e = (EnumConstant)f;
	    return e.hasBody ();
	}
	return false;
    }

    protected boolean hasDeclarations (Rule rule) {
	return rule.size () > 2;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (" {\n");
	declarations.forEach (d -> sb.append (d).append ("\n"));
	sb.append ("}\n");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	declarations.forEach (v::accept);
    }

    public List<TypeDeclaration> getInnerClasses () {
	return classDeclarations;
    }
}
