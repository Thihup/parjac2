package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.FieldInfo;
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

	    if (f instanceof UnqualifiedClassInstanceCreationExpression u) {
		if (u.hasBody ())
		    handleAnonymousClass (u);
	    } else if (f instanceof EnumConstant ec) {
		if (ec.hasBody ())
		    handleAnonymousClass (ec);
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

    private void handleAnonymousClass (AnonymousClass ac) {
	classDeclarations.add (ac);
	anonymousClasses.add (ac);
	ac.setAnonymousClassname (Integer.toString (anonymousClasses.size ()));
    }

    private boolean isLocalClass (ParseTreeNode parent, ParseTreeNode f) {
	return (f instanceof TypeDeclaration &&
		!(parent instanceof ClassBody || parent instanceof InterfaceBody));
    }

    private int getLocalNameCounter (String name) {
	return localNameCounter.compute (name, (k, v) -> (v ==  null) ? 1 : v + 1);
    }

    public Map<String, FieldInfo> getFields (List<? extends FieldDeclarationBase> fds, Context ctx) {
	Map<String, FieldInfo> ret = new LinkedHashMap<> ();  // keep things in order
	for (FieldDeclarationBase fd : fds) {
	    for (VariableDeclarator vd : fd.getVariableDeclarators ()) {
		String name = vd.getName ();
		if (ret.containsKey (name)) {
		    ctx.error (vd.position (), "Field with name %s already exists", name);
		} else {
		    Dims dims = vd.getDims ();
		    int rank = dims != null ? dims.rank () : 0;
		    ret.put (name, new FieldInfo (name, fd.position (), fd.flags (), fd.getType (), rank));
		}
	    }
	}
	return ret;
    }

    private record ParentToChild (ParseTreeNode parent, ParseTreeNode toCheck) {}
}
