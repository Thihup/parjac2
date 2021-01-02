package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parsetree.TokenNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class DistributorHelper {
    public static TypeDistributor getClassDistributor (List<TypeDeclaration> classDeclarations) {
	TypeDistributor td = new TypeDistributor ();
	td.addMapping (NormalClassDeclaration.class, classDeclarations);
	td.addMapping (EnumDeclaration.class, classDeclarations);
	td.addMapping (NormalInterfaceDeclaration.class, classDeclarations);
	td.addMapping (AnnotationTypeDeclaration.class, classDeclarations);
	td.skip (TokenNode.class);
	return td;
    }
}