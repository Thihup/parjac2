package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class EnhancedForStatement extends SyntaxTreeNode {
    private final LocalVariableDeclaration lv;
    private final ParseTreeNode expression;
    private final ParseTreeNode statement;

    public EnhancedForStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 2;
	if (children.get (2) instanceof LocalVariableDeclaration lv) {
	    // modern version allows initializers and multiple ids.
	    this.lv = lv;
	} else {
	    // java 11 and similar
	    List<ParseTreeNode> modifiers = rule.size () > 8 ? ((Multiple)children.get (i++)).get () : List.of ();
	    ParseTreeNode type = children.get (i++);
	    VariableDeclaratorId id = (VariableDeclaratorId)children.get (i++);
	    lv = new LocalVariableDeclaration (modifiers, type, new VariableDeclaratorList (new VariableDeclarator (id)));
	}
	i++; // :
	expression = children.get (i++);
	i++;
	statement = children.get (i);
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("for (").append (lv.getValue ()).append (" : ")
	    .append (expression).append (")").append (statement);
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	v.accept (lv);
	v.accept (expression);
	v.accept (statement);
    }
}
