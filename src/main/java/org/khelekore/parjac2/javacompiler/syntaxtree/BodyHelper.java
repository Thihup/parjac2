package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class BodyHelper {

    private final List<TypeDeclaration> classDeclarations;
    private final List<TypeDeclaration> localClasses;
    private final List<TypeDeclaration> anonymousClasses = new ArrayList<> ();
    private Map<String, Integer> localNameCounter = new HashMap<> ();

    public BodyHelper (List<TypeDeclaration> classDeclarations, List<TypeDeclaration> localClasses) {
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
		anonymousClasses.add (ac);
		ac.setAnonymousClassname (Integer.toString (anonymousClasses.size ()));
	    } else if (isLocalClass (pc.parent (), f)) {
		TypeDeclaration localClass = (TypeDeclaration)f;
		classDeclarations.add (localClass);
		localClasses.add (localClass);
		String name = localClass.getName ();
		localClass.setLocalName (getLocalNameCounter (name) + name);
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

    private boolean isAnonymousClass (ParseTreeNode f) {
	if (f instanceof UnqualifiedClassInstanceCreationExpression u) {
	    return u.hasBody ();
	} else if (f instanceof EnumConstant e) {
	    return e.hasBody ();
	}
	return false;
    }

    private boolean isLocalClass (ParseTreeNode parent, ParseTreeNode f) {
	return (!(f instanceof UnqualifiedClassInstanceCreationExpression) && f instanceof TypeDeclaration &&
		!(parent instanceof ClassBody || parent instanceof InterfaceBody));
    }

    private int getLocalNameCounter (String name) {
	return localNameCounter.compute (name, (k, v) -> (v ==  null) ? 1 : v + 1);
    }

    private record ParentToChild (ParseTreeNode parent, ParseTreeNode toCheck) {}
}
