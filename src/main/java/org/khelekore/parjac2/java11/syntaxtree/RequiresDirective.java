package org.khelekore.parjac2.java11.syntaxtree;

import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class RequiresDirective extends ModuleDirective {
    private final List<ParseTreeNode> modifiers;
    private final ModuleName moduleName;

    public RequiresDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 1;
	modifiers = (r.size () > 3) ? ((Multiple)children.get (i++)).get () : Collections.emptyList ();
	moduleName = (ModuleName)children.get (i);
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	sb.append ("requires ");
	if (!modifiers.isEmpty ())
	    sb.append (modifiers).append (" ");
	sb.append (moduleName).append (";");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	modifiers.forEach (v::accept);
	v.accept (moduleName);
    }
}
