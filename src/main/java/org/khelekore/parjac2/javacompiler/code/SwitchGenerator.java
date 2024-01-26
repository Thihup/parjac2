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

import org.khelekore.parjac2.javacompiler.IntLiteral;
import org.khelekore.parjac2.javacompiler.MethodContentGenerator;
import org.khelekore.parjac2.javacompiler.syntaxtree.BlockStatements;
import org.khelekore.parjac2.javacompiler.syntaxtree.BreakStatement;
import org.khelekore.parjac2.javacompiler.syntaxtree.CaseLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.CasePatternLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.DefaultLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHelper;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchBlock;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchBlockStatementGroup;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchExpression;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchLabel;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchLabelColon;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchPart;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchRule;
import org.khelekore.parjac2.javacompiler.syntaxtree.SwitchStatement;
import org.khelekore.parjac2.parsetree.ParseTreeNode;

import io.github.dmlloyd.classfile.CodeBuilder;
import io.github.dmlloyd.classfile.Label;
import io.github.dmlloyd.classfile.TypeKind;
import io.github.dmlloyd.classfile.instruction.SwitchCase;

public class SwitchGenerator {

    public static void handleSwitchStatement (MethodContentGenerator mcg, CodeBuilder cb, SwitchStatement ss) {
	handleSwitch (mcg, cb, ss.expression (), null, ss.block ());
    }

    public static void handleSwitchExpression (MethodContentGenerator mcg, CodeBuilder cb, SwitchExpression se) {
	handleSwitch (mcg, cb, se.expression (), FullNameHelper.type (se), se.block ());
    }

    private static void handleSwitch (MethodContentGenerator mcg, CodeBuilder cb, ParseTreeNode exp,
				      FullNameHandler resultType, SwitchBlock block) {
	IntCases ic = IntCases.tryCreate (cb, block);
	if (ic != null) {
	    handleIntSwitch (mcg, cb, exp, resultType, block, ic);
	} else {
	    handleObjectSwitch (mcg, cb, exp, resultType, block);
	}
    }

    private static void handleIntSwitch (MethodContentGenerator mcg, CodeBuilder cb, ParseTreeNode exp,
					 FullNameHandler resultType, SwitchBlock block, IntCases ic) {
	TypeKind tkTo = resultType == null ? TypeKind.VoidType : FullNameHelper.getTypeKind (resultType);

	mcg.handleStatements (cb, exp);

	// TODO: javac generates tableswitch in some cases, figure out when to use that instead.
	/* https://stackoverflow.com/questions/10287700/difference-between-jvms-lookupswitch-and-tableswitch
	   langtools/src/share/classes/com/sun/tools/javac/jvm/Gen.java:
	   long table_space_cost = 4 + ((long) hi - lo + 1); // words
	   long table_time_cost = 3; // comparisons
	   long lookup_space_cost = 3 + 2 * (long) nlabels;
	   long lookup_time_cost = nlabels;
	   int opcode =
	       nlabels > 0 &&
	       table_space_cost + 3 * table_time_cost <=
	       lookup_space_cost + 3 * lookup_time_cost
	       ?
	       tableswitch : lookupswitch;
	 */

	Label defaultTarget = cb.newLabel ();
	cb.lookupswitch (defaultTarget, ic.intCases);
	Label end = cb.newLabel ();
	mcg.registerJumpTargets ("", block, null, end);
	ic.part2target.forEach ((p, l) -> {
		cb.labelBinding (l);
		runHandler (cb, mcg, p.handler (), resultType, tkTo);
		possiblyAddGoto (cb, end, p.handler ());
	    });

	cb.labelBinding (defaultTarget);
	if (ic.defaultRule != null) {
	    runHandler (cb, mcg, ic.defaultRule.handler (), resultType, tkTo);
	}
	cb.labelBinding (end);
    }

    private static void possiblyAddGoto (CodeBuilder cb, Label end, ParseTreeNode handler) {
	if (handler instanceof BlockStatements bs) {
	    List<ParseTreeNode> ls = bs.statements ();
	    if (ls.size () > 0 && ls.get (ls.size () - 1) instanceof BreakStatement)
		cb.goto_ (end);
	} else {
	    cb.goto_ (end);
	}
    }

    private static void handleObjectSwitch (MethodContentGenerator mcg, CodeBuilder cb, ParseTreeNode exp,
					    FullNameHandler resultType, SwitchBlock block) {
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
	SwitchBlockInfo blockInfo = new SwitchBlockInfo (cb, block);
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

	TypeKind tkTo = resultType == null ? TypeKind.VoidType : FullNameHelper.getTypeKind (resultType);
	Label secondaryDefaultLabel = cb.newLabel ();
	Label end = cb.newLabel ();
	mcg.registerJumpTargets ("", block, null, end);
	cb.lookupswitch (secondaryDefaultLabel, blockInfo.secondarySwitchCases);
	blockInfo.secondaryOptions.forEach ((sc, srl) -> {
		cb.labelBinding (sc.target ());
		runHandler (cb, mcg, srl.sr.handler (), resultType, tkTo);
		cb.goto_ (end);
	    });
	cb.labelBinding (secondaryDefaultLabel);
	if (blockInfo.secondaryDefault != null) {
	    runHandler (cb, mcg, blockInfo.secondaryDefault.handler (), resultType, tkTo);
	}
	cb.labelBinding (end);
    }

    private static void runHandler (CodeBuilder cb, MethodContentGenerator mcg, ParseTreeNode handler,
				    FullNameHandler resultType, TypeKind tkTo) {
	mcg.handleStatements (cb, handler);
	if (resultType != null) {
	    FullNameHandler fn = FullNameHelper.type (handler);
	    CodeUtil.widenOrAutoBoxAsNeeded (cb, fn, resultType, tkTo);
	}
    }

    private record IntCases (List<SwitchCase> intCases, Map<SwitchPart, Label> part2target, SwitchPart defaultRule) {
	// empty

	public static IntCases tryCreate (CodeBuilder cb, SwitchBlock block) {
	    List<SwitchCase> intCases = new ArrayList<> ();
	    Map<SwitchPart, Label> part2target = new LinkedHashMap<> ();
	    SwitchPart defaultRule = null;

	    Map<SwitchPart, List<SwitchLabel>> part2labels = new LinkedHashMap<> ();

	    if (block instanceof SwitchBlock.SwitchBlockStatements sbs) {
		for (SwitchBlockStatementGroup sbsg : sbs.statementGroups ()) {
		    List<SwitchLabel> ls = new ArrayList<> ();
		    part2labels.put (sbsg, ls);
		    List<SwitchLabelColon> labels = sbsg.labels ();
		    for (SwitchLabelColon slc : labels)
			ls.add (slc.label ());
		}
	    } else if (block instanceof SwitchBlock.SwitchBlockRule sbr) {
		for (SwitchRule r : sbr.rules ())
		    part2labels.put (r, List.of (r.label ()));
	    }

	    for (Map.Entry<SwitchPart, List<SwitchLabel>> me : part2labels.entrySet ()) {
		SwitchPart part = me.getKey ();
		List<SwitchLabel> ls = me.getValue ();
		for (SwitchLabel label : ls) {
		    if (label instanceof CaseLabel cl) {
			Label l = cb.newLabel ();
			part2target.put (part, l);
			for (ParseTreeNode exp : cl.expressions ()) {
			    if (exp instanceof IntLiteral il) {
				SwitchCase sc = SwitchCase.of (il.intValue (), l);
				intCases.add (sc);
			    } else {
				return null;
			    }
			}
		    } else if (label instanceof DefaultLabel dl) {
			defaultRule = part;
		    } else if (label instanceof CasePatternLabel cpl) {
			return null;
		    }
		}
	    }
	    return new IntCases (intCases, part2target, defaultRule);
	}
    }

    private static class SwitchBlockInfo {
	private List<SwitchCase> primarySwitchCases = new ArrayList<> ();
	private LinkedHashMap<Integer, List<SwitchRuleLabel>> hashedOptions = new LinkedHashMap<> ();
	private Map<Integer, SwitchCase> hash2sc = new HashMap<> ();

	private List<SwitchCase> secondarySwitchCases = new ArrayList<> ();
	private LinkedHashMap<SwitchCase, SwitchRuleLabel> secondaryOptions = new LinkedHashMap<> ();
	private SwitchRule secondaryDefault;

	public SwitchBlockInfo (CodeBuilder cb, SwitchBlock block) {
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
