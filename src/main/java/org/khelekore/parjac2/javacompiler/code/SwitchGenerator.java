package org.khelekore.parjac2.javacompiler.code;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.CaseLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.CasePatternLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.DefaultLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchBlock;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchRule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.instruction.SwitchCase;

public class SwitchGenerator {
    public static void handleSwitchExpression (MethodContentGenerator mcg, CodeBuilder cb, SwitchExpression se) {
	ParseTreeNode exp = se.expression ();
	TypeKind kind = FullNameHelper.getTypeKind (FullNameHelper.type (exp));
	int expressionSlot = cb.allocateLocal (kind);
	mcg.handleStatements (cb, exp);
	cb.astore (expressionSlot);

	int lookupSlot = cb.allocateLocal (TypeKind.IntType);
	cb.iconst_m1 ();
	cb.istore (lookupSlot);
	cb.aload (expressionSlot);

	// String handling:
	ClassDesc owner = ConstantDescs.CD_String;
	String name = "hashCode";
	MethodTypeDesc type = MethodTypeDesc.ofDescriptor ("()I");
	cb.invokevirtual (owner, name, type);
	SwitchBlockInfo blockInfo = new SwitchBlockInfo (cb, se);
	Label nextLookup = cb.newLabel ();
	cb.lookupswitch (nextLookup, blockInfo.primarySwitchCases);

	MethodTypeDesc stringEqualsMtd = MethodTypeDesc.ofDescriptor ("(Ljava/lang/Object;)Z");
	Map.Entry<Integer, List<SwitchRuleLabel>> last = blockInfo.hashedOptions.lastEntry ();
	for (Map.Entry<Integer, List<SwitchRuleLabel>> me : blockInfo.hashedOptions.entrySet ()) {
	    List<SwitchRuleLabel> ls = me.getValue ();
	    cb.labelBinding (blockInfo.hash2sc.get (me.getKey ()).target ());

	    SwitchRuleLabel lastSRL = ls.getLast ();
	    for (SwitchRuleLabel srl : ls) {
		cb.aload (expressionSlot);
		cb.ldc ((ConstantDesc)srl.constant);
		cb.invokevirtual (owner, "equals", stringEqualsMtd);
		cb.ifeq (nextLookup);
		CodeUtil.handleInt (cb, srl.secondaryValue);
		cb.istore (lookupSlot);
		if (!me.equals (last) || lastSRL != srl)
		    cb.goto_ (nextLookup);
	    }
	}

	cb.labelBinding (nextLookup);
	cb.iload (lookupSlot);

	FullNameHandler toType = FullNameHelper.type (se);
	TypeKind tkTo = FullNameHelper.getTypeKind (toType);
	Label secondaryDefaultLabel = cb.newLabel ();
	Label end = cb.newLabel ();
	cb.lookupswitch (secondaryDefaultLabel, blockInfo.secondarySwitchCases);
	blockInfo.secondaryOptions.forEach ((sc, srl) -> {
		cb.labelBinding (sc.target ());
		ParseTreeNode handler = srl.sr.handler ();
		mcg.handleStatements (cb, handler);
		FullNameHandler fn = FullNameHelper.type (handler);
		CodeUtil.widenOrAutoBoxAsNeeded (cb, fn, toType, tkTo);
		cb.goto_ (end);
	    });
	cb.labelBinding (secondaryDefaultLabel);
	if (blockInfo.secondaryDefault != null) {
	    ParseTreeNode handler = blockInfo.secondaryDefault.handler ();
	    mcg.handleStatements (cb, handler);
	    FullNameHandler fn = FullNameHelper.type (handler);
	    CodeUtil.widenOrAutoBoxAsNeeded (cb, fn, toType, tkTo);
	}
	cb.labelBinding (end);
    }

    private static class SwitchBlockInfo {
	private List<SwitchCase> primarySwitchCases = new ArrayList<> ();
	private LinkedHashMap<Integer, List<SwitchRuleLabel>> hashedOptions = new LinkedHashMap<> ();
	private Map<Integer, SwitchCase> hash2sc = new HashMap<> ();

	private List<SwitchCase> secondarySwitchCases = new ArrayList<> ();
	private LinkedHashMap<SwitchCase, SwitchRuleLabel> secondaryOptions = new LinkedHashMap<> ();
	private SwitchRule secondaryDefault;

	public SwitchBlockInfo (CodeBuilder cb, SwitchExpression se) {
	    SwitchBlock block = se.block ();
	    SwitchBlock.SwitchBlockRule sbr = (SwitchBlock.SwitchBlockRule)block;
	    List<SwitchRule> rules = sbr.rules ();
	    int secondaryValue = 0;
	    for (SwitchRule r : rules) {
		SwitchLabel label = r.label ();
		if (label instanceof CaseLabel cl) {
		    for (ParseTreeNode exp : cl.expressions ()) {
			SwitchRuleLabel srl = new SwitchRuleLabel (r, exp.getValue (), secondaryValue++);
			int hash = srl.constant.hashCode ();
			List<SwitchRuleLabel> ls = hashedOptions.computeIfAbsent (hash, ArrayList::new);
			if (ls.isEmpty ()) {
			    SwitchCase sc = SwitchCase.of (hash, cb.newLabel ());
			    primarySwitchCases.add (sc);
			    hash2sc.put (hash, sc);
			}
			ls.add (srl);

			SwitchCase sc = SwitchCase.of (srl.secondaryValue, cb.newLabel ());
			secondarySwitchCases.add (sc);
			secondaryOptions.put (sc, srl);
		    }
		} else if (label instanceof DefaultLabel dl) {
		    secondaryDefault = r;
		} else if (label instanceof CasePatternLabel cpl) {
		    throw new IllegalStateException ("Case pattern labels not yet handled");
		}
	    }
	}
    }

    private record SwitchRuleLabel (SwitchRule sr, Object constant, int secondaryValue) { /* empty */ }
}
