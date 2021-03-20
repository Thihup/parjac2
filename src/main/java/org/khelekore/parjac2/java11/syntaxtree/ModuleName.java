
package org.khelekore.parjac2.java11.syntaxtree;

import java.util.List;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ModuleName extends DottedName {
    public ModuleName (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	super (rule, n, children);
    }
}