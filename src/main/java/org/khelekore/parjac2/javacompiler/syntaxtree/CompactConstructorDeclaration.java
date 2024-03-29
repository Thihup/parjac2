package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.attribute.ExceptionsAttribute;

public class CompactConstructorDeclaration extends ConstructorBase {
    private final List<ParseTreeNode> modifiers;
    private final String name;
    private final ConstructorBody body;

    public CompactConstructorDeclaration (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	// {ConstructorModifier} SimpleTypeName ConstructorBody
	int i = 0;
	modifiers = (rule.size () > 2) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	name = ((Identifier)children.get (i++)).getValue ();
	body = (ConstructorBody)children.get (i++);
	flags = Flags.ACC_PUBLIC;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	modifiers.forEach (m -> sb.append (m).append (" "));
	sb.append (name).append (" ").append (body.getValue ());
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (body);
    }

    @Override public List<? extends ParseTreeNode> getAnnotations () {
	return null;
    }

    @Override public TypeParameters getTypeParameters () {
	return null;
    }

    @Override public String getName () {
	return name;
    }

    @Override public ReceiverParameter getReceiverParameter () {
	return null;
    }

    @Override public FormalParameterList getFormalParameterList () {
	return null;  // TODO: not sure how to deal with this, should probably return RecordComponentList
    }

    @Override public ExplicitConstructorInvocation explicitConstructorInvocation () {
	return null;
    }

    @Override public List<ParseTreeNode> statements () {
	return body.statements ();
    }

    @Override public ConstructorBody body () {
	return body;
    }

    @Override public ExceptionsAttribute exceptions () {
	return null;
    }

    @Override public List<ClassType> thrownTypes () {
	return null;
    }
}
