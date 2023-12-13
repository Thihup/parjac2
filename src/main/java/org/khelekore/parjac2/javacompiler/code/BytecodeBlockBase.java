package org.khelekore.parjac2.javacompiler.code;

import java.util.List;

import org.khelekore.parjac2.javacompiler.syntaxtree.BytecodeBlock;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class BytecodeBlockBase implements ParseTreeNode, BytecodeBlock {
    private final ParsePosition pos;
    public BytecodeBlockBase (ParsePosition pos) {
	this.pos = pos;
    }

    @Override public Object getId () {
	return getClass ().getName ();
    }

    @Override public Object getValue () {
	return getClass ().getName ();
    }

    @Override public boolean isRule () {
	return false;
    }

    @Override public boolean isToken () {
	return false;
    }

    @Override public List<ParseTreeNode> getChildren () {
	return List.of ();
    }

    @Override public ParsePosition position () {
	return pos;
    }

    @Override public void visitChildNodes (NodeVisitor v) {
	// empty
    }
}
