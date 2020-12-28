package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class SingleElementAnnotation extends Annotation {
    private TypeName typename;
    private ParseTreeNode value;

    public SingleElementAnnotation (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	this.typename = (TypeName)children.get (1);
	value = children.get (3);
    }

    @Override public Object getValue() {
	return "@" + typename + "(" + value + ")";
    }
}
