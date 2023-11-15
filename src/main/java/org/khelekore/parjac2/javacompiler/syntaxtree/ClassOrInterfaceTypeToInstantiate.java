package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

// 	{Annotation} 'Identifier' {'.' {Annotation} 'Identifier'} [TypeArgumentsOrDiamond]
public class ClassOrInterfaceTypeToInstantiate extends SyntaxTreeNode {
    private final List<AnnotatedIdentifier> ids;
    private final ClassType type;
    private final ParseTreeNode types;

    public ClassOrInterfaceTypeToInstantiate (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	ids = new ArrayList<> ();
	int i = 0;
	List<Annotation> annotations = List.of ();
	if (children.get (i) instanceof Multiple)
	    annotations = getAnnotations ((Multiple)children.get (i++));
	Identifier id = (Identifier)children.get (i++);
	ids.add (new AnnotatedIdentifier (n.position (), annotations, id));
	if (children.size () > i && children.get (i) instanceof Multiple) {
	    Multiple z = (Multiple)children.get (i++);
	    List<ParseTreeNode> ls = z.getChildren ();
	    for (int j = 0; j < ls.size (); j += 2) {
		ParseTreeNode ptn = ls.get (j + 1);
		if (ptn instanceof Multiple) {
		    annotations = getAnnotations ((Multiple)ptn);
		    id = (Identifier)children.get (++j);
		} else {
		    annotations = List.of ();
		    id = (Identifier)ptn;
		}
		ids.add (new AnnotatedIdentifier (ptn.position (), annotations, id));
	    }
	}
	types = (rule.size () > i) ? children.get (i) : null;

	ParsePosition pos = n.position ();
	List<SimpleClassType> scts = new ArrayList<> ();
	ids.forEach (ai -> scts.add (new SimpleClassType (pos, ai.getIdentifier (), null)));
	type = new ClassType (pos, scts);
    }

    private List<Annotation> getAnnotations (Multiple z) {
	return z.get ();
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	for (AnnotatedIdentifier ai : ids) {
	    if (sb.length () > 0)
		sb.append (".");
	    sb.append (ai.getValue ());
	}
	if (types != null)
	    sb.append (types);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	ids.forEach (v::accept);
	if (types != null)
	    v.accept (types);
    }

    public ClassType type () {
	return type;
    }

    public FullNameHandler fullName () {
	return type.fullName ();
    }
}
