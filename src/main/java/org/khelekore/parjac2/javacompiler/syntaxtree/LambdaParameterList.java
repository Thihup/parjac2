package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.NodeVisitor;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public abstract class LambdaParameterList<T> extends SyntaxTreeNode {
    private final List<T> params;

    private LambdaParameterList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children,
				 Function<ParseTreeNode, T> nodeConverter) {
	super (n.position ());
	if (rule.size () == 1) {
	    params = List.of (nodeConverter.apply (children.get (0)));
	} else {
	    params = new ArrayList<> ();
	    params.add (nodeConverter.apply (children.get (0)));
	    Multiple z = (Multiple)children.get (1);
	    for (int i = 1; i < z.size (); i += 2)
		params.add (nodeConverter.apply (z.get (i)));
	}
    }

    public static LambdaParameterList<String> forIdentifier (Rule rule, ParseTreeNode n, List<ParseTreeNode> children,
							     Function<ParseTreeNode, String> nodeConverter) {
	return new StringLambdaParameterList (rule, n, children, nodeConverter);
    }

    public static LambdaParameterList<LambdaParameter>
    forParameters (Rule rule, ParseTreeNode n, List<ParseTreeNode> children,
		   Function<ParseTreeNode, LambdaParameter> nodeConverter) {
	return new ParamLambdaParameterList (rule, n, children, nodeConverter);
    }

    @Override public Object getValue () {
	return params;
    }

    public List<T> getParams () {
	return params;
    }

    public int numberOfArguments () {
	return params.size ();
    }

    public abstract String parameterName (int i);

    public abstract FullNameHandler parameter (int i);

    public static class StringLambdaParameterList extends LambdaParameterList<String> {
	public StringLambdaParameterList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children,
					  Function<ParseTreeNode, String> nodeConverter) {
	    super (rule, n, children, nodeConverter);
	}

	@Override public void visitChildNodes (NodeVisitor v) {
	    // nothing
	}

	@Override public String parameterName (int i) {
	    return getParams ().get (i);
	}

	@Override public FullNameHandler parameter (int i) {
	    return null;
	}
    }

    public static class ParamLambdaParameterList extends LambdaParameterList<LambdaParameter> {
	public ParamLambdaParameterList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children,
					 Function<ParseTreeNode, LambdaParameter> nodeConverter) {
	    super (rule, n, children, nodeConverter);
	}

	@Override public void visitChildNodes (NodeVisitor v) {
	    getParams ().forEach (v::accept);
	}

	@Override public String parameterName (int i) {
	    return getParams ().get (i).name ();
	}

	@Override public FullNameHandler parameter (int i) {
	    return getParams ().get (i).type ();
	}
    }
}
