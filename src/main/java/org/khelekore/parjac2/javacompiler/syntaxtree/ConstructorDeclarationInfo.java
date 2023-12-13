package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.lang.constant.MethodTypeDesc;
import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.MethodInfo;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

public interface ConstructorDeclarationInfo extends MethodInfo {

    List<? extends ParseTreeNode> getAnnotations ();

    TypeParameters getTypeParameters ();

    @Override int flags ();

    /** Get the simple name of the class this constructor belongs to */
    String getName ();

    @Override default String name () {
	return "<init>";
    }

    ExplicitConstructorInvocation explicitConstructorInvocation ();

    List<ParseTreeNode> statements ();

    ReceiverParameter getReceiverParameter ();

    FormalParameterList getFormalParameterList ();

    ConstructorBody body ();

    void owner (FullNameHandler owner);

    @Override default MethodTypeDesc methodTypeDesc () {
	return ClassDescUtils.methodTypeDesc (getFormalParameterList (), result ());
    }

    @Override default int numberOfArguments () {
	FormalParameterList ls = getFormalParameterList ();
	if (ls == null)
	    return 0;
	return ls.size ();
    }

    // return type
    @Override default FullNameHandler result () {
	return FullNameHandler.VOID;
    }

    @Override default FullNameHandler parameter (int i) {
	return FullNameHelper.type (getFormalParameterList ().getParameters ().get (i));
    }

    @Override default String signature () {
	return null; // TODO: can we have a signature here?
    }
}
