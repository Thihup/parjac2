package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.FieldInfo;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class UnqualifiedClassInstanceCreationExpression extends AnonymousClass {
    private final TypeArguments types;
    private final ClassOrInterfaceTypeToInstantiate type;
    private final ArgumentList args;
    private final ClassBody body;

    public UnqualifiedClassInstanceCreationExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
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

    @Override public boolean isLocalClass (TypeDeclaration td) {
	return body == null ? false : body.isLocalClass (td);
    }

    @Override public int flags () {
	// TODO: fill in flags
	return 0;
    }

    @Override public ClassType getSuperClass () {
	return type.type ();
    }

    @Override public Map<String, FieldInfo> getFields () {
	return body.getFields ();
    }

    @Override public List<MethodDeclaration> getMethods () {
	return body.getMethods ();
    }

    @Override public List<ConstructorDeclaration> getConstructors () {
	return List.of ();
    }

    @Override public List<SyntaxTreeNode> getInstanceInitializers () {
	return body.getInstanceInitializers ();
    }

    @Override public List<SyntaxTreeNode> getStaticInitializers () {
	return body.getStaticInitializers ();
    }

    public FullNameHandler type () {
	return type.fullName ();
    }
}
