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

public class InterfaceBody extends SyntaxTreeNode {
    private List<ParseTreeNode> declarations;

    private List<ParseTreeNode> constantDeclarations = new ArrayList<> ();
    private List<ParseTreeNode> interfaceMethodDeclarations = new ArrayList<> ();
    private List<ParseTreeNode> classDeclarations = new ArrayList<> ();
    private List<ParseTreeNode> interfaceDeclarations = new ArrayList<> ();

    private static Map<Class<?>, BiConsumer<InterfaceBody, ParseTreeNode>> distributor = new HashMap<> ();

    static {
	distributor.put (ConstantDeclaration.class, (ib, n) -> ib.constantDeclarations.add (n));
	distributor.put (InterfaceMethodDeclaration.class, (ib, n) -> ib.interfaceMethodDeclarations.add (n));

	distributor.put (NormalClassDeclaration.class, (ib, n) -> ib.classDeclarations.add (n));
	distributor.put (EnumDeclaration.class, (ib, n) -> ib.classDeclarations.add (n));

	distributor.put (NormalInterfaceDeclaration.class, (ib, n) -> ib.interfaceDeclarations.add (n));
	distributor.put (AnnotationTypeDeclaration.class, (ib, n) -> ib.interfaceDeclarations.add (n));

	distributor.put (TokenNode.class, (ib, n) -> {/*nothing*/}); // ';'
    }

    public InterfaceBody (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	if (rule.size () > 2) {
	    declarations = ((Multiple)children.get (1)).get ();
	} else {
	    declarations = Collections.emptyList ();
	}

	declarations.forEach (this::distribute);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append (" {\n");
	declarations.forEach (n -> sb.append (n).append ("\n"));
	sb.append ("}");
	return sb.toString ();
    }

    private void distribute (ParseTreeNode n) {
	distributor.getOrDefault (n.getClass (), InterfaceBody::handleBadType).accept (this, n);
    }

    private static void handleBadType (InterfaceBody cb, ParseTreeNode n) {
	throw new IllegalStateException ("Unhandled type: " + n.getClass () + ": " + n);
    }
}
