package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class UnqualifiedClassInstanceCreationExpression extends AnonymousClass {
    private final TypeArguments types;
    private final ClassOrInterfaceTypeToInstantiate type;
    private final ArgumentList args;
    private final ClassBody body;

    public UnqualifiedClassInstanceCreationExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 1;
	types = (children.get (i) instanceof TypeArguments) ? (TypeArguments)children.get (i++) : null;
	type = (ClassOrInterfaceTypeToInstantiate)children.get (i++);
	i++;
	args = (children.get (i) instanceof ArgumentList) ? (ArgumentList)children.get (i++) : null;
	i++;
	body = (rule.size () > i) ? (ClassBody)children.get (i) : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("new ");
	if (types != null)
	    sb.append (types);
	sb.append (type);
	sb.append ("(");
	if (args != null)
	    sb.append (args);
	sb.append (")");
	if (body != null)
	    sb.append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (types != null)
	    v.accept (types);
	v.accept (type);
	if (args != null)
	    v.accept (args);
	if (body != null)
	    v.accept (body);
    }

    public boolean hasBody () {
	return body != null;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body == null ? List.of () : body.getInnerClasses ();
    }

    @Override public int getFlags () {
	// TODO: fill in flags
	return 0;
    }
}
