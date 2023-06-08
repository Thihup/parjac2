package org.khelekore.parjac2.javacompiler;

import java.nio.file.Path;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ParsedEntry {
    private DirAndPath dirAndPath;
    private ParseTreeNode root;

    public ParsedEntry (DirAndPath dirAndPath, ParseTreeNode root) {
	this.dirAndPath = dirAndPath;
	this.root = root;
    }

    public DirAndPath getDirAndPath () {
	return dirAndPath;
    }

    public Path getOrigin () {
	return dirAndPath.getFile ();
    }

    public ParseTreeNode getRoot () {
	return root;
    }
}
