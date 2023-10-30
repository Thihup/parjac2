package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.FieldInfo;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.javacompiler.JavaTokens;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.util.TypeDistributor;

public class ClassBody extends SyntaxTreeNode {
    protected List<ParseTreeNode> declarations; // all of them

    // either plain blocks or field assignments
    protected List<SyntaxTreeNode> instanceInitializers = new ArrayList<> ();
    protected List<StaticInitializer> staticInitializers = new ArrayList<> ();
    protected List<ConstructorDeclaration> constructorDeclarations = new ArrayList<> ();

    // Change this, currently FieldDeclaration have VariableDeclaratorList in them, we want a list of fields.
    protected List<FieldDeclaration> fieldDeclarations = new ArrayList<> ();
    protected List<MethodDeclaration> methodDeclarations = new ArrayList<> ();

    // inner classes, enums, records, interfaces and annotations
    protected List<TypeDeclaration> classDeclarations = new ArrayList<> ();
    protected List<TypeDeclaration> localClasses = new ArrayList<> ();

    private Map<String, FieldInfo> nameToField;

    public ClassBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	declarations = hasDeclarations (rule) ? ((Multiple)children.get (1)).get () : Collections.emptyList ();
	TypeDistributor td = DistributorHelper.getClassDistributor (classDeclarations);
	td.addMapping (Block.class, instanceInitializers);
	td.addMapping (StaticInitializer.class, staticInitializers);
	td.addMapping (ConstructorDeclaration.class, constructorDeclarations);
	td.<FieldDeclaration>addMapping (FieldDeclaration.class, t -> handleFields (ctx.getTokens (), t));
	td.addMapping (MethodDeclaration.class, methodDeclarations);
	addAdditionalMappings (td);
	declarations.forEach (td::distribute);
	BodyHelper bh = new BodyHelper (classDeclarations, localClasses);
	bh.findInnerClasses (this, declarations);
	nameToField = bh.getFields (fieldDeclarations, ctx);
    }

    protected void addAdditionalMappings (TypeDistributor td) {
	// empty
    }

    private void handleFields (JavaTokens javaTokens, FieldDeclaration fd) {
	fieldDeclarations.add (fd);
	List<VariableDeclarator> ls = fd.getVariableDeclarators ();
	for (VariableDeclarator vd : ls) {
	    String name = vd.getName ();
	    if (vd.hasInitializer ()) {
		instanceInitializers.add (new Assignment (new Identifier (javaTokens.IDENTIFIER, name, vd.position ()),
							  javaTokens.EQUAL,
							  vd.getInitializer ()));
	    }
	}
    }

    protected boolean hasDeclarations (Rule rule) {
	return rule.size () > 2;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{\n");
	declarations.forEach (d -> sb.append (d).append ("\n"));
	sb.append ("}\n");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	declarations.forEach (v::accept);
    }

    public List<TypeDeclaration> getInnerClasses () {
	return classDeclarations;
    }

    public boolean isLocalClass (TypeDeclaration td) {
	return localClasses.contains (td);
    }

    public Map<String, FieldInfo> getFields () {
	return nameToField;
    }

    public List<MethodDeclaration> getMethods () {
	return methodDeclarations;
    }

    public List<ConstructorDeclaration> getConsructors () {
	return constructorDeclarations;
    }

    public List<SyntaxTreeNode> getInstanceInitializers () {
	return instanceInitializers;
    }

    public List<StaticInitializer> getStaticInitializers () {
	return staticInitializers;
    }
}
