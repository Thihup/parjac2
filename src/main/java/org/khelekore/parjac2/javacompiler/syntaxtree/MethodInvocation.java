package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.MethodInfo;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class MethodInvocation extends SyntaxTreeNode {
    private final ParseTreeNode on;
    private final boolean isSuper;
    private final TypeArguments types;
    private final UntypedMethodInvocation mi;
    private MethodInfo info;
    private boolean returnValueDiscarded = true;
    private Map<String, FullNameHandler> genericTypes;

    public MethodInvocation (ParsePosition pos, ParseTreeNode on, boolean isSuper,
			     TypeArguments types, UntypedMethodInvocation mi) {
	super (pos);
	this.on = on;
	this.types = types;
	this.isSuper = isSuper;
	this.mi = mi;
    }

    public MethodInvocation (ParsePosition pos, ParseTreeNode on, String methodName, ParseTreeNode... args) {
	super (pos);
	this.on = on;
	this.types = null;
	this.isSuper = false;
	mi = new UntypedMethodInvocation (pos, methodName, args);
    }

    public void info (MethodInfo info, Map<String, FullNameHandler> genericTypes) {
	this.info = info;
	this.genericTypes = genericTypes;
    }

    public MethodInfo info () {
	return info;
    }

    public FullNameHandler owner () {
	return info != null ? info.owner () : null;
    }

    public FullNameHandler result () {
	return info != null ? info.result () : null;
    }

    public FullNameHandler genericResult () {
	return info != null ? info.result () : null;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	if (on != null)
	    sb.append (on).append (".");
	if (isSuper)
	    sb.append ("super.");
	if (types != null)
	    sb.append (types);
	sb.append (mi);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (on != null)
	    v.accept (on);
	if (types != null)
	    v.accept (types);
	v.accept (mi);
    }

    public ParseTreeNode getOn () {
	return on;
    }

    public TypeArguments types () {
	return types;
    }

    public boolean isSuper () {
	return isSuper;
    }

    public String getMethodName () {
	return mi.getMethodName ();
    }

    public List<ParseTreeNode> getArguments () {
	return mi.getArguments ();
    }

    public boolean isCallToStaticMethod () {
	return info.isStatic ();
    }

    public boolean returnValueDiscarded () {
	return returnValueDiscarded;
    }

    public void returnValueUsed () {
	returnValueDiscarded = false;
    }
}
