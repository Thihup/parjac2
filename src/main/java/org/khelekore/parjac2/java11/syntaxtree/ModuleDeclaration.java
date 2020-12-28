package org.khelekore.parjac2.java11.syntaxtree;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.khelekore.parjac2.java11.Identifier;
import org.khelekore.parjac2.java11.Java11Tokens;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ModuleDeclaration extends SyntaxTreeNode {
    private List<Annotation> annotations;
    private boolean isOpen;
    private List<String> identifiers;
    private List<ModuleDirective> directives;

    public ModuleDeclaration (Path path, Java11Tokens java11Tokens, Rule rule,
			      ParseTreeNode n, List<ParseTreeNode> children) {
	super (n.getPosition ());
	int i = 0;
	if (children.get (i) instanceof Multiple)
	    annotations = ((Multiple)children.get (i++)).get ();
	else
	    annotations = Collections.emptyList ();

	isOpen = (rule.get (i) == java11Tokens.OPEN.getId ());
	if (isOpen)
	    i++;

	i++; // 'module'
	identifiers = new ArrayList<> ();
	identifiers.add (((Identifier)children.get (i++)).getValue ());
	if (children.get (i) instanceof Multiple) {
	    Multiple z = (Multiple)children.get (i++);
	    for (int j = 1, e = z.size (); j < e; j += 2)
		identifiers.add (((Identifier)z.get (j)).getValue ());
	}
	i++; // '{'
	if (children.get (i) instanceof Multiple)
	    directives = ((Multiple)children.get (i++)).get ();
	else
	    directives = Collections.emptyList ();

	i++; // '}'
    }

    @Override public Object getValue() {
	StringBuilder sb = new StringBuilder ();
	if (!annotations.isEmpty ())
	    sb.append (annotations).append (" ");
	if (isOpen)
	    sb.append ("open ");
	sb.append ("module ");
	sb.append (identifiers.get (0));
	for (int i = 1; i < identifiers.size (); i++)
	    sb.append (".").append (identifiers.get (i));
	sb.append ("{\n");
	for (ModuleDirective d : directives)
	    sb.append (d).append ("\n");
	sb.append ("}\n");
	return sb.toString ();
    }
}
