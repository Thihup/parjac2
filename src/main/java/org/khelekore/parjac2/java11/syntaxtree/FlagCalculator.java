package org.khelekore.parjac2.java11.syntaxtree;

import java.util.ArrayList;
import java.util.List;

import org.khelekore.parjac2.java11.Context;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class FlagCalculator {
    private final int baseValue;
    private List<Combination> addDefaultUnless = new ArrayList<> ();
    private List<Integer> invalidCombinations = new ArrayList<> ();
    private List<Combination> invalidIf = new ArrayList<> ();

    public FlagCalculator (int baseValue) {
	this.baseValue = baseValue;
    }

    public void addDefaultUnless (int value, int flagMask) {
	addDefaultUnless.add (new Combination (value, flagMask));
    }

    // Add invalid combination, only one of the bits may be set at any time
    public void addInvalid (int flagMask) {
	invalidCombinations.add (flagMask);
    }

    public void addInvalidIf (int trigger, int mask) {
	invalidIf.add (new Combination (trigger, mask));
    }

    public int calculate (Context ctx, List<ParseTreeNode> modifiers, ParsePosition pos) {
	int flags = 0;

	for (ParseTreeNode m : modifiers) {
	    int modifierFlag = 0;
	    if (m instanceof TokenNode) {
		TokenNode tn = (TokenNode)m;
		Token t = tn.getToken ();
		modifierFlag = ctx.getFlagValue (t);
		if ((flags & modifierFlag) != 0)
		    ctx.error (tn.getPosition (), "Duplicate modifier %s found", t.getName ());
		flags |= modifierFlag;
	    }
	}
	flags |= baseValue;
	for (Combination c : addDefaultUnless)
	    if (!c.hasFlags (flags))
		flags |= c.value;

	if (!modifiers.isEmpty ()) {
	    for (Integer ic : invalidCombinations) {
		int clash = flags & ic;
		if (Integer.bitCount (clash) > 1) {
		    ctx.error (pos, "Invalid modifier combination, only one of (%s) is allowed",
			       ctx.getTokenNameString (clash));
		}
	    }

	    for (Combination c : invalidIf) {
		if ((flags & c.value) > 0) {
		    int clash = flags & c.flagMask;
		    if (Integer.bitCount (clash) > 0) {
			ctx.error (pos, "Invalid modifier combination, (%s) not allowed while %s",
				   ctx.getTokenNameString (clash), ctx.getToken (c.value).getName ());
		    }
		}
	    }
	}

	return flags;
    }

    private static class Combination {
	private final int value;
	private final int flagMask;

	public Combination (int value, int flagMask) {
	    this.value = value;
	    this.flagMask = flagMask;
	}

	public boolean hasFlags (int flags) {
	    return (flags & flagMask) != 0;
	}
    }
}