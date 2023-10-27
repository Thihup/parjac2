package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LocalVariableDeclarationStatement extends SyntaxTreeNode {
    private final LocalVariableDeclaration decl;

    public LocalVariableDeclarationStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	decl = (LocalVariableDeclaration)children.get (0);
    }

    @Override public Object getValue() {
	return decl + ";";
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (decl);
    }
}
