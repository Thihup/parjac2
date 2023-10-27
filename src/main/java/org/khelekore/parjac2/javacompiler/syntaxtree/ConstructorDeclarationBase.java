package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public interface ConstructorDeclarationBase {

    ParsePosition getPosition ();

    List<? extends ParseTreeNode> getAnnotations ();

    TypeParameters getTypeParameters ();

    int flags ();

    String getName ();

    List<ParseTreeNode> getStatements ();

    ReceiverParameter getReceiverParameter ();

    FormalParameterList getFormalParameterList ();
}
