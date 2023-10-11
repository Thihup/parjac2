package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumBody extends SyntaxTreeNode {
    private final EnumConstantList constants;
    private final EnumBodyDeclarations declarations;
    private List<TypeDeclaration> innerClasses;

    public EnumBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
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
	    declarations = null;
	}
	innerClasses = new ArrayList<> ();
	if (constants != null) {
	    constants.getConstants ().stream ().filter (EnumConstant::hasBody).forEach (innerClasses::add);
	}
	if (declarations != null)
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
	return declarations.isLocalClass (td);
    }

    public void setParents (EnumDeclaration ed) {
	if (constants != null)
	    constants.getConstants ().forEach (c -> c.setParent (ed));
    }
}
