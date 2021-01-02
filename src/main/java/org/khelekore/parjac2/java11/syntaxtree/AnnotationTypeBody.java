package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class AnnotationTypeBody extends SyntaxTreeNode {
    private final List<ParseTreeNode> declarations;

    private List<ParseTreeNode> annotationTypeElementDeclarations = new ArrayList<> ();
    private List<ParseTreeNode> constantDeclarations = new ArrayList<> ();
    private List<TypeDeclaration> classDeclarations = new ArrayList<> ();

    public AnnotationTypeBody (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 2) {
	    declarations = ((Multiple)children.get (1)).get ();
	} else {
	    declarations = List.of ();
	}
	TypeDistributor td = DistributorHelper.getClassDistributor (classDeclarations);
	td.addMapping (AnnotationTypeElementDeclaration.class, annotationTypeElementDeclarations);
	td.addMapping (ConstantDeclaration.class, constantDeclarations);
	declarations.forEach (td::distribute);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	if (!declarations.isEmpty ())
	    sb.append (declarations);
	sb.append ("}");
	return sb.toString ();
    }

    public List<TypeDeclaration> getInnerClasses () {
	return classDeclarations;
    }
}
