package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class Superinterfaces extends SyntaxTreeNode {
    private List<ClassType> types;
    public Superinterfaces (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	InterfaceTypeList l = (InterfaceTypeList)children.get (1);
	types = l.getTypes ();
    }

    @Override public Object getValue () {
	return "implements " + types.stream ().map (x -> x.toString ()).collect (Collectors.joining (", "));
    }
}
