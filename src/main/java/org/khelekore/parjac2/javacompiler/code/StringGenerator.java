package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.DirectMethodHandleDesc;
import java.lang.constant.DynamicCallSiteDesc;
import java.lang.constant.MethodHandleDesc;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import org.khelekore.parjac2.javacompiler.ClassDescUtils;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.TwoPartExpression;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;

public class StringGenerator {

    public static void handleStringConcat (MethodContentGenerator mcg, CodeBuilder cb, TwoPartExpression two) {
	List<ParseTreeNode> parts = getAllStringParts (two);
	List<ClassDesc> types = new ArrayList<> ();
	StringBuilder recipeBuilder = new StringBuilder ();
	for (ParseTreeNode p : parts) {
	    if (CodeUtil.isLiteral (p)) {
		recipeBuilder.append (p.getValue ());
	    } else {
		mcg.handleStatements (cb, p);
		types.add (ClassDescUtils.getClassDesc (FullNameHelper.type (p)));
		recipeBuilder.append ("\1");
	    }
	}
	String recipe = recipeBuilder.toString ();

	DirectMethodHandleDesc.Kind kind = DirectMethodHandleDesc.Kind.STATIC;
	ClassDesc owner = ClassDesc.ofInternalName ("java/lang/invoke/StringConcatFactory");
	String name = "makeConcatWithConstants";
	MethodTypeDesc lookupMethodType =
	    MethodTypeDesc.ofDescriptor ("(" +
					 "Ljava/lang/invoke/MethodHandles$Lookup;" +
					 "Ljava/lang/String;" +                        // name
					 "Ljava/lang/invoke/MethodType;" +             // concat type
					 "Ljava/lang/String;" +                        // recipe
					 "[Ljava/lang/Object;" +                       // constants
					 ")" +
					 "Ljava/lang/invoke/CallSite;");
	DirectMethodHandleDesc bootstrapMethod =
	    MethodHandleDesc.ofMethod (kind, owner, name, lookupMethodType);
	ClassDesc ret = ClassDescUtils.getClassDesc (FullNameHandler.JL_STRING);
	MethodTypeDesc invocationType = MethodTypeDesc.of (ret, types);
	ConstantDesc[] bootstrapArgs = {recipe};
	DynamicCallSiteDesc ref = DynamicCallSiteDesc.of (bootstrapMethod, name, invocationType, bootstrapArgs);
	cb.invokedynamic (ref);
    }

    private static List<ParseTreeNode> getAllStringParts (TwoPartExpression tp) {
	List<ParseTreeNode> res = new ArrayList<> ();
	Deque<ParseTreeNode> queue = new ArrayDeque<> ();
	queue.addLast (tp.part1 ());
	queue.addLast (tp.part2 ());
	while (!queue.isEmpty ()) {
	    ParseTreeNode p = queue.removeFirst ();
	    if (p instanceof TwoPartExpression t && CodeUtil.isString (t)) {
		queue.addFirst (t.part2 ());
		queue.addFirst (t.part1 ());
	    } else {
		res.add (p);
	    }
	}
	return res;
    }
}
