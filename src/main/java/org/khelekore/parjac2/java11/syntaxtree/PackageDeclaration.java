package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.StringHelper;

public class PackageDeclaration extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final List<String> nameParts;

    public PackageDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
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
    }

    @Override public Object getValue () {
	return annotations + (annotations.isEmpty () ? "" : " ") +
	    "package " + StringHelper.dotted (nameParts) + ";\n";
    }
}
