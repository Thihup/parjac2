package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class Annotation extends SyntaxTreeNode {
    private TypeName typename;

    public Annotation (ParsePosition pos, List<ParseTreeNode> children) {
	super (pos);
	this.typename = (TypeName)children.get (1);
    }

    public TypeName getTypeName () {
	return typename;
    }

    public static <T extends ParseTreeNode> List<T> getAnnotations (List<T> modifiers) {
	return modifiers.stream ().filter (m -> m instanceof Annotation).toList ();
    }
}
