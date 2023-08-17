package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.javacompiler.JavaTokens;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class ClassBody extends SyntaxTreeNode {
    protected List<ParseTreeNode> declarations; // all of them

    // either plain blocks or field assignments
    protected List<SyntaxTreeNode> instanceInitializers = new ArrayList<> ();
    protected List<StaticInitializer> staticInitializers = new ArrayList<> ();
    protected List<ConstructorDeclaration> constructorDeclarations = new ArrayList<> ();

    // Change this, currently FieldDeclaration have VariableDeclaratorList in them, we want a list of fields.
    protected List<FieldDeclaration> fieldDeclarations = new ArrayList<> ();
    protected List<MethodDeclaration> methodDeclarations = new ArrayList<> ();

    // inner classes, enums, interfaces and annotations
    protected List<TypeDeclaration> classDeclarations = new ArrayList<> ();

    public ClassBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	declarations = hasDeclarations (rule) ? ((Multiple)children.get (1)).get () : Collections.emptyList ();
	TypeDistributor td = DistributorHelper.getClassDistributor (classDeclarations);
	td.addMapping (Block.class, instanceInitializers);
	td.addMapping (StaticInitializer.class, staticInitializers);
	td.addMapping (ConstructorDeclaration.class, constructorDeclarations);
	td.<FieldDeclaration>addMapping (FieldDeclaration.class, t -> handleFields (ctx.getTokens (), t, instanceInitializers));
	td.addMapping (MethodDeclaration.class, methodDeclarations);
	addAdditionalMappings (td);
	declarations.forEach (td::distribute);

	declarations.forEach (this::findInnerClasses);
    }

    protected void addAdditionalMappings (TypeDistributor td) {
	// empty
    }

    private void handleFields (JavaTokens javaTokens, FieldDeclaration t, List<SyntaxTreeNode> initList) {
	FieldDeclaration fd = (FieldDeclaration)t;
	List<VariableDeclarator> ls = fd.getVariableDeclarators ();
	for (VariableDeclarator vd : ls) {
	    String name = vd.getName ();
	    // TODO: put field in map or something
	    if (vd.hasInitializer ()) {
		initList.add (new Assignment (new Identifier (javaTokens.IDENTIFIER, name, vd.getPosition ()),
					      javaTokens.EQUAL,
					      vd.getInitializer ()));
	    }
	}
    }

    private void findInnerClasses (ParseTreeNode n) {
	// We do a depth first search for classes.
	Deque<ParseTreeNode> dq = new ArrayDeque<> ();
	dq.addFirst (n);
	while (!dq.isEmpty ()) {
	    ParseTreeNode f = dq.removeFirst ();
	    if (isAnonymousClass (f)) {
		AnonymousClass ac = (AnonymousClass)f;
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
