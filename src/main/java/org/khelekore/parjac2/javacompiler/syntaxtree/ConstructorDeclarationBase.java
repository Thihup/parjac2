package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public interface ConstructorDeclarationBase {

    List<? extends ParseTreeNode> getAnnotations ();

    TypeParameters getTypeParameters ();

    List<ParseTreeNode> getStatements ();

    ReceiverParameter getReceiverParameter ();

    FormalParameterList getFormalParameterList ();
}
