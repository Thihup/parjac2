package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class SyntaxTreeNode implements ParseTreeNode {
    private final ParsePosition pos;

    public SyntaxTreeNode (ParsePosition pos) {
	this.pos = pos;
    }

    @Override public Object getId () {
	return getClass ().getSimpleName ();
    }

    @Override public boolean isRule () {
	return true;
    }

    @Override public boolean isToken () {
	return false;
    }

    @Override public List<ParseTreeNode> getChildren () {
	List<ParseTreeNode> ret = new ArrayList<> ();
	visitChildNodes (ret::add);
	return ret;
    }

    @Override public ParsePosition position () {
	return pos;
    }

    @Override public String toString () {
	return getValue ().toString ();
    }
}
