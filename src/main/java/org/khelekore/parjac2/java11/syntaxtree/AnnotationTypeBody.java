package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class AnnotationTypeBody extends SyntaxTreeNode {
    private final List<ParseTreeNode> declarations;

    private List<ParseTreeNode> annotationTypeElementDeclarations = new ArrayList<> ();
    private List<ParseTreeNode> constantDeclarations = new ArrayList<> ();
    private List<TypeDeclaration> classDeclarations = new ArrayList<> ();

    // inner classes, enums, interfaces and annotations
    private static Map<Class<?>, BiConsumer<AnnotationTypeBody, ParseTreeNode>> distributor = new HashMap<> ();

    static {
	distributor.put (AnnotationTypeElementDeclaration.class, (at, n) -> at.annotationTypeElementDeclarations.add (n));
	distributor.put (ConstantDeclaration.class, (at, n) -> at.constantDeclarations.add (n));

	distributor.put (NormalClassDeclaration.class, (at, n) -> at.classDeclarations.add ((TypeDeclaration)n));
	distributor.put (EnumDeclaration.class, (at, n) -> at.classDeclarations.add ((TypeDeclaration)n));
	distributor.put (NormalInterfaceDeclaration.class, (at, n) -> at.classDeclarations.add ((TypeDeclaration)n));
	distributor.put (AnnotationTypeDeclaration.class, (at, n) -> at.classDeclarations.add ((TypeDeclaration)n));

	distributor.put (TokenNode.class, (at, n) -> {/*nothing*/}); // ';'
    }

    public AnnotationTypeBody (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 2) {
	    declarations = ((Multiple)children.get (1)).get ();
	} else {
	    declarations = List.of ();
	}
	declarations.forEach (this::distribute);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	if (!declarations.isEmpty ())
	    sb.append (declarations);
	sb.append ("}");
	return sb.toString ();
    }

    private void distribute (ParseTreeNode n) {
	distributor.getOrDefault (n.getClass (), AnnotationTypeBody::handleBadType).accept (this, n);
    }

    private static void handleBadType (AnnotationTypeBody cb, ParseTreeNode n) {
	throw new IllegalStateException ("Unhandled type: " + n.getClass () + ": " + n);
    }

    public List<TypeDeclaration> getInnerClasses () {
	return classDeclarations;
    }
}
