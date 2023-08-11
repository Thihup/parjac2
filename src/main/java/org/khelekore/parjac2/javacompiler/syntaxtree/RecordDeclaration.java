package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Identifier;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class RecordDeclaration extends TypeDeclaration {
    private final List<ParseTreeNode> modifiers;
    private final String id;
    private final TypeParameters typeParameters;
    private final RecordHeader recordHeader;
    private final Superinterfaces classImplements;
    private final RecordBody body;

    private static FlagCalculator flagCalculator = FlagCalculator.SIMPLE_ACCESS;

    public RecordDeclaration (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());

	// {ClassModifier} 'record' TypeIdentifier [TypeParameters] RecordHeader [ClassImplements] RecordBody
	int i = 0;
	if (children.get (0) instanceof Multiple) {
	    modifiers = ((Multiple)children.get (i++)).get ();
	} else {
	    modifiers = Collections.emptyList ();
	}
	i++; // record
	id = ((Identifier)children.get (i++)).getValue ();
	ParseTreeNode tn = children.get (i++);
	if (tn instanceof TypeParameters tp) {
	    typeParameters = tp;
	    recordHeader = (RecordHeader)children.get (i++);
	} else {
	    typeParameters = null;
	    recordHeader = (RecordHeader)tn;
	}
	tn = children.get (i++);
	if (tn instanceof Superinterfaces ci) {
	    classImplements = ci;
	    body = (RecordBody)children.get (i++);
	} else {
	    classImplements = null;
	    body = (RecordBody)tn;
	}
	flags = flagCalculator.calculate (ctx, modifiers, getPosition ());
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append ("record ").append (id).append (" ");
	if (typeParameters != null)
	    sb.append (typeParameters.getValue ());
	sb.append (recordHeader);
	if (classImplements != null)
	    sb.append (classImplements.getValue ());
	sb.append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);;
	if (typeParameters != null)
	    v.accept (typeParameters);
	v.accept (recordHeader);
	if (classImplements != null)
	    v.accept (classImplements);
	v.accept (body);

    }

    @Override public String getName () {
	return id;
    }

    @Override public List<TypeDeclaration> getInnerClasses () {
	return body.getInnerClasses ();
    }
}
