package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SimpleClassType extends SyntaxTreeNode {
    private final List<Annotation> annotations;
    private final String id;
    private final TypeArguments typeArguments;

    public SimpleClassType (Path path, Rule rule, ParseTreeNode sct, List<ParseTreeNode> children) {
	super (sct.getPosition ());
	int i = 0;
	ParseTreeNode n = children.get (i++);
	if (n instanceof Multiple) {
	    annotations = ((Multiple)n).get ();
	    n = children.get (i++);
	} else {
	    annotations = Collections.emptyList ();
	}
	id = ((Identifier)n).getValue ();
	typeArguments = rule.size () > i ? (TypeArguments)children.get (i) : null;
    }

    public SimpleClassType (ParsePosition pos, List<Annotation> annotations, String id, TypeArguments tas) {
	super (pos);
	this.annotations = annotations;
	this.id = id;
	this.typeArguments = tas;
    }

    @Override public Object getValue () {
	String a = annotations.stream ().map (x -> x.toString ()).collect (Collectors.joining (" "));
	return a + id + (typeArguments != null ? typeArguments : "");
    }
}
