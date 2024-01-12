package org.khelekore.parjac2.javacompiler;

import org.khelekore.parjac2.parsetree.ParseTreeNode;

public class ConstantValueCalculator {
    public static ParseTreeNode add (JavaTokens javaTokens, NumericLiteral n1, NumericLiteral n2) {
	if (n1 instanceof DoubleLiteral || n2 instanceof DoubleLiteral) {
	    return new DoubleLiteral (javaTokens.DOUBLE_LITERAL, n1.doubleValue () + n2.doubleValue (), n1.position ());
	} else if (n1 instanceof FloatLiteral || n2 instanceof FloatLiteral) {
	    return new FloatLiteral (javaTokens.FLOAT_LITERAL, n1.floatValue () + n2.floatValue (), n1.position ());
	} else if (n1 instanceof LongLiteral || n2 instanceof LongLiteral) {
	    return new LongLiteral (javaTokens.LONG_LITERAL, n1.longValue () + n2.longValue (), n1.position ());
	} else { // both IntLiteral
	    return new IntLiteral (javaTokens.INT_LITERAL, n1.intValue () + n2.intValue (), n1.position ());
	}
    }

    public static ParseTreeNode subtract (JavaTokens javaTokens, NumericLiteral n1, NumericLiteral n2) {
	if (n1 instanceof DoubleLiteral || n2 instanceof DoubleLiteral) {
	    return new DoubleLiteral (javaTokens.DOUBLE_LITERAL, n1.doubleValue () - n2.doubleValue (), n1.position ());
	} else if (n1 instanceof FloatLiteral || n2 instanceof FloatLiteral) {
	    return new FloatLiteral (javaTokens.FLOAT_LITERAL, n1.floatValue () - n2.floatValue (), n1.position ());
	} else if (n1 instanceof LongLiteral || n2 instanceof LongLiteral) {
	    return new LongLiteral (javaTokens.LONG_LITERAL, n1.longValue () - n2.longValue (), n1.position ());
	} else { // both IntLiteral
	    return new IntLiteral (javaTokens.INT_LITERAL, n1.intValue () - n2.intValue (), n1.position ());
	}
    }

    public static ParseTreeNode multiply (JavaTokens javaTokens, NumericLiteral n1, NumericLiteral n2) {
	if (n1 instanceof DoubleLiteral || n2 instanceof DoubleLiteral) {
	    return new DoubleLiteral (javaTokens.DOUBLE_LITERAL, n1.doubleValue () * n2.doubleValue (), n1.position ());
	} else if (n1 instanceof FloatLiteral || n2 instanceof FloatLiteral) {
	    return new FloatLiteral (javaTokens.FLOAT_LITERAL, n1.floatValue () * n2.floatValue (), n1.position ());
	} else if (n1 instanceof LongLiteral || n2 instanceof LongLiteral) {
	    return new LongLiteral (javaTokens.LONG_LITERAL, n1.longValue () * n2.longValue (), n1.position ());
	} else { // both IntLiteral
	    return new IntLiteral (javaTokens.INT_LITERAL, n1.intValue () * n2.intValue (), n1.position ());
	}
    }

    public static ParseTreeNode divide (JavaTokens javaTokens, NumericLiteral n1, NumericLiteral n2) {
	if (n1 instanceof DoubleLiteral || n2 instanceof DoubleLiteral) {
	    return new DoubleLiteral (javaTokens.DOUBLE_LITERAL, n1.doubleValue () / n2.doubleValue (), n1.position ());
	} else if (n1 instanceof FloatLiteral || n2 instanceof FloatLiteral) {
	    return new FloatLiteral (javaTokens.FLOAT_LITERAL, n1.floatValue () / n2.floatValue (), n1.position ());
	} else if (n1 instanceof LongLiteral || n2 instanceof LongLiteral) {
	    return new LongLiteral (javaTokens.LONG_LITERAL, n1.longValue () / n2.longValue (), n1.position ());
	} else { // both IntLiteral
	    return new IntLiteral (javaTokens.INT_LITERAL, n1.intValue () / n2.intValue (), n1.position ());
	}
    }

    public static ParseTreeNode bitNegate (JavaTokens javaTokens, NumericLiteral n) {
	if (n instanceof LongLiteral)
	    return new LongLiteral (javaTokens.LONG_LITERAL, ~n.longValue (), n.position ());
	return new IntLiteral (javaTokens.INT_LITERAL, ~n.intValue (), n.position ());
    }
}
