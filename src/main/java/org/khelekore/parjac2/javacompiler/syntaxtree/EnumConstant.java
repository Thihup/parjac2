package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.FieldInfo;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnumConstant extends AnonymousClass {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final ArgumentList args;
    private final ClassBody body;
    private EnumDeclaration ed;

    public EnumConstant (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	id = ((Identifier)children.get (i++)).getValue ();
	if (rule.size () > i && rule.get (i) == ctx.getTokens ().LEFT_PARENTHESIS.getId ()) {
	    i++;
	    if (rule.size () > i && rule.get (i) != ctx.getTokens ().RIGHT_PARENTHESIS.getId ()) {
		args = (ArgumentList)children.get (i++);
	    } else {
		args = null;
	    }
	    i++; // ')'
	} else {
	    args = null;
	}
	body = (rule.size () > i) ? (ClassBody)children.get (i) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (id);
	if (args != null)
	    sb.append ("(").append (args).append (")");
	if (body != null)
	    sb.append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	if (args != null)
	    v.accept (args);
	if (body != null)
	    v.accept (body);
    }

    @Override public String getName () {
	return id;
    }

    public boolean hasBody () {
	return body != null;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body == null ? List.of () : body.getInnerClasses ();
    }

    @Override public boolean isLocalClass (TypeDeclaration td) {
	return body == null ? false : body.isLocalClass (td);
    }

    @Override public int flags () {
	return Flags.ACC_FINAL;
    }

    public void setParent (EnumDeclaration ed) {
	this.ed = ed;
    }

    public EnumDeclaration getParent () {
	return ed;
    }

    @Override public Map<String, FieldInfo> getFields () {
	return body.getFields ();
    }

    public List<MethodDeclaration> getMethods () {
	return body.getMethods ();
    }

    public List<ConstructorDeclaration> getConsructors () {
	return body.getConsructors ();
    }

    public List<SyntaxTreeNode> getInstanceInitializers () {
	return body.getInstanceInitializers ();
    }

    public List<StaticInitializer> getStaticInitializers () {
	return body.getStaticInitializers ();
    }
}
