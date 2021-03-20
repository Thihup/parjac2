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
    private Combination addDefaultUnless;
    private List<Integer> invalidCombinations = new ArrayList<> ();

    public FlagCalculator (int baseValue) {
	this.baseValue = baseValue;
    }

    public void addDefaultUnless (int value, int flagMask) {
	addDefaultUnless = new Combination (value, flagMask);
    }

    // Add invalid combination, only one of the bits may be set at any time
    public void addInvalid (int flagMask) {
	invalidCombinations.add (flagMask);
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
	if (addDefaultUnless != null && !addDefaultUnless.hasFlags (flags))
	    flags |= addDefaultUnless.value;

	if (!modifiers.isEmpty ()) {
	    for (Integer ic : invalidCombinations) {
		int clash = flags & ic;
		int bits = Integer.bitCount (clash);
		if (bits > 1) {
		    ctx.error (pos,
			       "Invalid modifier combination, only one of (%s) is allowed",
			       getFlagString (ctx, clash));
		}
	    }
	}

	return flags;
    }

    private String getFlagString (Context ctx, int mask) {
	StringBuilder sb = new StringBuilder ();
	while (mask > 0) {
	    int first = Integer.lowestOneBit (mask);
	    mask &= ~first;
	    if (sb.length () > 0)
		sb.append (", ");
	    sb.append (ctx.getToken (first).getName ());
	}
	return sb.toString ();
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