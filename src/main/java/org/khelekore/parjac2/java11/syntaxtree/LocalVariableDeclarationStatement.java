package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class LocalVariableDeclarationStatement extends SyntaxTreeNode {
    private final LocalVariableDeclaration decl;

    public LocalVariableDeclarationStatement (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	decl = (LocalVariableDeclaration)children.get (0);
    }

    @Override public Object getValue() {
	return decl + ";";
    }
}
