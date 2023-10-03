package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ExtendsInterfaces extends SyntaxTreeNode {
    private final InterfaceTypeList interfaceTypes;

    public ExtendsInterfaces (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	interfaceTypes = (InterfaceTypeList)children.get (1);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("extends ").append (interfaceTypes);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (interfaceTypes);
    }

    public List<ClassType> getTypes () {
	return interfaceTypes.getTypes ();
    }
}
