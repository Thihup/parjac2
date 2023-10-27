package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class RecordHeader extends SyntaxTreeNode {

    private List<RecordComponent> recordComponents;

    public RecordHeader (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.position ());
	if (rule.size () == 2) {
	    recordComponents = List.of ();
	} else {
	    RecordComponentList ls = (RecordComponentList)children.get (1);
	    recordComponents = ls.get ();
	}
    }

    @Override public Object getValue () {
	StringBuilder sb = new StringBuilder ();
	sb.append ("(");
	for (int i = 0; i < recordComponents.size (); i++) {
	    if (i > 0)
		sb.append (", ");
	    sb.append (recordComponents.get (i));
	}
	sb.append (")");
	return sb.toString ();
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	recordComponents.forEach (v::accept);
    }

    public List<RecordComponent> getRecordComponents () {
	return recordComponents;
    }
}
