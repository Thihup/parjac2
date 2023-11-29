package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.javacompiler.Flags;
import org.khelekore.parjac2.javacompiler.MethodInfo;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodDeclarationBase extends FlaggedBase implements MethodInfo {

    private final List<ParseTreeNode> modifiers;
    private final MethodHeader header;
    protected final ParseTreeNode body; // either ';' or a Block.
    private FullNameHandler owner;

    //  {MethodModifier} MethodHeader MethodBody
    public MethodDeclarationBase (Context ctx, Rule rule, ParseTreeNode n,
				  List<ParseTreeNode> children, FlagCalculator flagCalculator) {
	super (n.position ());
	int i = 0;
	modifiers = (rule.size () > 2) ? ((Multiple)children.get (i++)).get () : List.of ();
	header = (MethodHeader)children.get (i++);
	body = children.get (i);
	flags = flagCalculator.calculate (ctx, modifiers, position ());
	if (header.isVarArgs ())
	    flags |= Flags.ACC_VARARGS;
    }

    public MethodDeclarationBase (ParsePosition pos, int flags, String name, ParseTreeNode result, Block body) {
	super (pos);
	modifiers = List.of ();
	header = new MethodHeader (pos, name, result);
	this.body = body;
	this.flags = flags;
    }

    public void owner (FullNameHandler owner) {
	this.owner = owner;
    }

    @Override public FullNameHandler owner () {
	return owner;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (header).append (" ").append (body);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (header);
	v.accept (body);
    }

    public List<ParseTreeNode> getModifiers () {
	return modifiers;
    }

    public boolean isAbstract () {
	return Flags.isAbstract (flags);
    }

    public TypeParameters getTypeParameters () {
	return header.getTypeParameters ();
    }

    public List<ParseTreeNode> getAnnotations () {
	List<ParseTreeNode> ls1 = Annotation.getAnnotations (modifiers);
	List<ParseTreeNode> ls2 = header.getAnnotations ();
	if (ls2 == null)
	    return ls1;
	if (ls1 == null)
	    return ls2;
	ArrayList<ParseTreeNode> ret = new ArrayList<> (ls1.size () + ls2.size ());
	ret.addAll (ls1);
	ret.addAll (ls2);
	return ret;
    }

    public ParseTreeNode getResult () {
	return header.getResult ();
    }

    @Override public MethodTypeDesc methodTypeDesc () {
	return ClassDescUtils.methodTypeDesc (getFormalParameterList (), result ());
    }

    @Override public FullNameHandler result () {
	return FullNameHelper.type (getResult ());
    }

    @Override public FullNameHandler parameter (int i) {
	return FullNameHelper.type (getFormalParameterList ().getParameters ().get (i));
    }

    @Override public String name () {
	return header.getName ();
    }

    public ReceiverParameter getReceiverParameter () {
	return header.getReceiverParameter ();
    }

    public FormalParameterList getFormalParameterList () {
	return header.getFormalParameterList ();
    }

    @Override public int numberOfArguments () {
	FormalParameterList ls = getFormalParameterList ();
	return ls == null ? 0 : ls.size ();
    }

    public Dims getDims () {
	return header.getDims ();
    }

    public Throws getThrows () {
	return header.getThrows ();
    }

    public ParseTreeNode getMethodBody () {
	return body;
    }
}
