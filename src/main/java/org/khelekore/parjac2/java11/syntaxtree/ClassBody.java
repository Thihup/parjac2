package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class ClassBody extends SyntaxTreeNode {
    protected List<ParseTreeNode> declarations; // all of them

    protected List<ParseTreeNode> instanceInitializers = new ArrayList<> ();
    protected List<ParseTreeNode> staticInitializers = new ArrayList<> ();
    protected List<ParseTreeNode> constructorDeclarations = new ArrayList<> ();
    protected List<ParseTreeNode> fieldDeclarations = new ArrayList<> ();
    protected List<ParseTreeNode> methodDeclarations = new ArrayList<> ();
    protected List<ParseTreeNode> classDeclarations = new ArrayList<> ();
    protected List<ParseTreeNode> interfaceDeclarations = new ArrayList<> ();

    private static Map<Class<?>, BiConsumer<ClassBody, ParseTreeNode>> distributor = new HashMap<> ();

    static {
	distributor.put (Block.class, (cb, n) -> cb.instanceInitializers.add (n));
	distributor.put (StaticInitializer.class, (cb, n) -> cb.staticInitializers.add (n));
	distributor.put (ConstructorDeclaration.class, (cb, n) -> cb.constructorDeclarations.add (n));
	distributor.put (FieldDeclaration.class, (cb, n) -> cb.fieldDeclarations.add (n));
	distributor.put (MethodDeclaration.class, (cb, n) -> cb.methodDeclarations.add (n));

	distributor.put (NormalClassDeclaration.class, (cb, n) -> cb.classDeclarations.add (n));
	distributor.put (EnumDeclaration.class, (cb, n) -> cb.classDeclarations.add (n));

	distributor.put (NormalInterfaceDeclaration.class, (cb, n) -> cb.interfaceDeclarations.add (n));
	distributor.put (AnnotationTypeDeclaration.class, (cb, n) -> cb.interfaceDeclarations.add (n));

	distributor.put (TokenNode.class, (cb, n) -> {/*nothing*/}); // ';'
    }

    public ClassBody (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	declarations = hasDeclarations (rule) ? ((Multiple)children.get (1)).get () : Collections.emptyList ();
	declarations.forEach (this::distribute);
    }

    protected boolean hasDeclarations (Rule rule) {
	return rule.size () > 2;
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append (" {\n");
	declarations.forEach (d -> sb.append (d).append ("\n"));
	sb.append ("}\n");
	return sb.toString ();
    }

    private void distribute (ParseTreeNode n) {
	distributor.getOrDefault (n.getClass (), ClassBody::handleBadType).accept (this, n);
    }

    private static void handleBadType (ClassBody cb, ParseTreeNode n) {
	throw new IllegalStateException ("Unhandled type: " + n.getClass () + ": " + n);
    }
}
