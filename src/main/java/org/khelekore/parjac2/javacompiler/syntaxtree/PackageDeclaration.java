package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.StringHelper;

public class PackageDeclaration extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final List<String> nameParts;
    private final String dottedName;

    public PackageDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 0;
	if (children.get (i) instanceof Multiple) {
	    Multiple z = (Multiple)children.get (i++);
	    annotations = z.get ();
	} else {
	    annotations = Collections.emptyList ();
	}
	i++;
	nameParts = new ArrayList<> ();
	String firstPart = ((Identifier)children.get (i++)).getValue ();
	nameParts.add (firstPart);
	if (children.size () > i + 1) {
	    Multiple z = (Multiple)children.get (i);
	    for (int j = 1, e = z.size (); j < e; j += 2)
		nameParts.add (((Identifier)z.get (j)).getValue ());
	}
	dottedName = nameParts.stream ().collect (Collectors.joining ("."));
    }

    public PackageDeclaration (ParsePosition pos, List<Annotation> annotations, List<String> nameParts) {
	super (pos);
	this.annotations = annotations == null ? List.of () : annotations;
	this.nameParts = nameParts;
	dottedName = nameParts.stream ().collect (Collectors.joining ("."));
    }

    @Override public Object getValue () {
	return annotations + (annotations.isEmpty () ? "" : " ") +
	    "package " + StringHelper.dotted (nameParts) + ";\n";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	annotations.forEach (v::accept);
    }

    public String getName () {
	return dottedName;
    }

    @Override public boolean equals (Object o) {
	if (o == this)
	    return true;
	if (o == null)
	    return false;
	if (o.getClass () != getClass ())
	    return false;
	PackageDeclaration pd = (PackageDeclaration)o;
	return Objects.equals (annotations, pd.annotations) && nameParts.equals (pd.nameParts);
    }
}
