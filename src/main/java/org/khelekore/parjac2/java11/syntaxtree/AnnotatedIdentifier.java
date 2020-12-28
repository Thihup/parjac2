package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.java11.Identifier;

public class AnnotatedIdentifier {
    private final List<Annotation> annotations;
    private final String id;

    public AnnotatedIdentifier (List<Annotation> annotations, Identifier id) {
	this.annotations = annotations;
	this.id = id.getValue ();
    }

    public List<Annotation> getAnnotations () {
	return annotations;
    }

    public String getId () {
	return id;
    }
}
