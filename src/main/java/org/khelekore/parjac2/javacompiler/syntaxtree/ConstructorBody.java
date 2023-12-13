package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.javacompiler.Context;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstructorBody extends SyntaxTreeNode {
    private ExplicitConstructorInvocation eci;
    private BlockStatements statements;

    public ConstructorBody (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	int i = 1;
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("ExplicitConstructorInvocation"))
	    eci = (ExplicitConstructorInvocation)children.get (i++);
	if (rule.get (i) == ctx.getGrammar ().getRuleGroupId ("BlockStatements"))
	    statements = (BlockStatements)children.get (i);
    }

    public ConstructorBody (ParsePosition pos, ExplicitConstructorInvocation eci, List<ParseTreeNode> statements) {
	super (pos);
	this.eci = eci;
	this.statements = new BlockStatements (pos, statements);
    }

    public ExplicitConstructorInvocation explicitConstructorInvocation () {
	return eci;
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("{\n");
	if (eci != null)
	    sb.append (eci);
	if (statements != null)
	    sb.append (statements);
	sb.append ("\n}");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	if (eci != null)
	    v.accept (eci);
	if (statements != null)
	    v.accept (statements);
    }

    public List<ParseTreeNode> statements () {
	return statements == null ? List.of () : statements.statements ();
    }
}
