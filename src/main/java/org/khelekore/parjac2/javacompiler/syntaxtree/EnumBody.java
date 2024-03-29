package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.FieldInfo;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumBody extends SyntaxTreeNode {
    private final EnumConstantList constants;
    private final EnumBodyDeclarations declarations;
    private List<TypeDeclaration> innerClasses;

    public EnumBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 1;
	if (children.get (i) instanceof EnumConstantList) {
	    constants = (EnumConstantList)children.get (i++);
	} else {
	    constants = null;
	}
	if (rule.get (i) == ctx.getTokens ().COMMA.getId ())
	    i++;
	if (children.get (i) instanceof EnumBodyDeclarations) {
	    declarations = (EnumBodyDeclarations)children.get (i);
	} else {
	    declarations = new EnumBodyDeclarations (n.position ());
	}
	innerClasses = new ArrayList<> ();
	if (constants != null) {
	    constants.constants ().stream ().filter (EnumConstant::hasBody).forEach (innerClasses::add);
	}
	innerClasses.addAll (declarations.getInnerClasses ());
	// TODO: do we not need to use BodyHelper here?
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{");
	if (constants != null)
	    sb.append (constants);
	if (declarations != null)
	    sb.append (declarations);
	sb.append ("}");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (constants != null)
	    v.accept (constants);
	if (declarations != null)
	    v.accept (declarations);
    }

    public List<TypeDeclaration> getInnerClasses () {
	return innerClasses;
    }

    public boolean isLocalClass (TypeDeclaration td) {
	if (declarations == null)
	    return false;
	return declarations.isLocalClass (td);
    }

    public void setParents (EnumDeclaration ed) {
	if (constants != null)
	    constants.constants ().forEach (c -> c.setParent (ed));
    }

    public Map<String, FieldInfo> getFields () {
	return declarations.getFields ();
    }

    public List<MethodDeclaration> getMethods () {
	return declarations.getMethods ();
    }

    public List<ConstructorDeclaration> getConsructors () {
	return declarations.getConsructors ();
    }

    public List<SyntaxTreeNode> getInstanceInitializers () {
	return declarations.getInstanceInitializers ();
    }

    public List<SyntaxTreeNode> getStaticInitializers () {
	return declarations.getStaticInitializers ();
    }

    public List<EnumConstant> constants () {
	if (constants == null)
	    return List.of ();
	return constants.constants ();
    }
}

