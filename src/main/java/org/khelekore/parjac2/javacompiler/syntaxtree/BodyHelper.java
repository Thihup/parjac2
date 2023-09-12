package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Set;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class BodyHelper {

    private final List<TypeDeclaration> classDeclarations;
    private final Set<TypeDeclaration> localClasses;

    public BodyHelper (List<TypeDeclaration> classDeclarations, Set<TypeDeclaration> localClasses) {
	this.classDeclarations = classDeclarations;
	this.localClasses = localClasses;
    }

    public void findInnerClasses (ParseTreeNode parent, List<ParseTreeNode> declarations) {
	declarations.forEach (d -> findInnerClasses (new ParentToChild (parent, d)));
    }

    private void findInnerClasses (ParentToChild ptc) {
	// We do a depth first search for classes.
	Deque<ParentToChild> dq = new ArrayDeque<> ();
	dq.addFirst (ptc);
	while (!dq.isEmpty ()) {
	    ParentToChild pc = dq.removeFirst ();
	    ParseTreeNode f = pc.toCheck ();
	    if (isAnonymousClass (f)) {
		AnonymousClass ac = (AnonymousClass)f;
		classDeclarations.add (ac);
	    } else if (isLocalClass (pc.parent (), f)) {
		classDeclarations.add ((TypeDeclaration)f);
		localClasses.add ((TypeDeclaration)f);
	    } else {
		int end = dq.size ();
		// since we we want to add all the child nodes first in deque we first add them last
		// and them move them first so that things end up in wanted order
		f.visitChildNodes (cn -> dq.addLast (new ParentToChild (f, cn)));
		int diff = dq.size () - end; // we added this many child nodes
		for (int i = 0; i < diff; i++)
		    dq.addFirst (dq.removeLast ());
	    }
	}
    }

    private boolean isLocalClass (ParseTreeNode parent, ParseTreeNode f) {
	return (f instanceof TypeDeclaration td &&
		!(parent instanceof ClassBody));
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

    private record ParentToChild (ParseTreeNode parent, ParseTreeNode toCheck) {}
}
