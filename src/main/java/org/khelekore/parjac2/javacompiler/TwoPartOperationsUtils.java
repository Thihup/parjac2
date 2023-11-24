package org.khelekore.parjac2.javacompiler;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.khelekore.parjac2.parser.Token;

import io.github.dmlloyd.classfile.CodeBuilder;

public class TwoPartOperationsUtils {
    public static Map<Token, Consumer<CodeBuilder>> getIntMap (JavaTokens javaTokens) {
	Map<Token, Consumer<CodeBuilder>> ret = new HashMap<> ();
	ret.put (javaTokens.PLUS, CodeBuilder::iadd);
	ret.put (javaTokens.MINUS, CodeBuilder::isub);
	ret.put (javaTokens.MULTIPLY, CodeBuilder::imul);
	ret.put (javaTokens.DIVIDE, CodeBuilder::idiv);
	ret.put (javaTokens.REMAINDER, CodeBuilder::irem);
	ret.put (javaTokens.LEFT_SHIFT, CodeBuilder::ishl);
	ret.put (javaTokens.RIGHT_SHIFT, CodeBuilder::ishr);
	ret.put (javaTokens.RIGHT_SHIFT_UNSIGNED, CodeBuilder::iushr);
	ret.put (javaTokens.AND, CodeBuilder::iand);
	ret.put (javaTokens.OR, CodeBuilder::ior);
	ret.put (javaTokens.XOR, CodeBuilder::ixor);
	ret.put (javaTokens.TILDE, cb -> { cb.iconst_m1 (); cb.ixor (); });
	return ret;
    }

    public static Map<Token, Consumer<CodeBuilder>> getLongMap (JavaTokens javaTokens) {
	Map<Token, Consumer<CodeBuilder>> ret = new HashMap<> ();
	ret.put (javaTokens.PLUS, CodeBuilder::ladd);
	ret.put (javaTokens.MINUS, CodeBuilder::lsub);
	ret.put (javaTokens.MULTIPLY, CodeBuilder::lmul);
	ret.put (javaTokens.DIVIDE, CodeBuilder::ldiv);
	ret.put (javaTokens.REMAINDER, CodeBuilder::lrem);
	ret.put (javaTokens.LEFT_SHIFT, CodeBuilder::lshl);
	ret.put (javaTokens.RIGHT_SHIFT, CodeBuilder::lshr);
	ret.put (javaTokens.RIGHT_SHIFT_UNSIGNED, CodeBuilder::lushr);
	ret.put (javaTokens.AND, CodeBuilder::land);
	ret.put (javaTokens.OR, CodeBuilder::lor);
	ret.put (javaTokens.XOR, CodeBuilder::lxor);
	ret.put (javaTokens.TILDE, cb -> { cb.ldc (-1L); cb.lxor (); });
	return ret;
    }

    public static Map<Token, Consumer<CodeBuilder>> getDoubleMap (JavaTokens javaTokens) {
	Map<Token, Consumer<CodeBuilder>> ret = new HashMap<> ();
	ret.put (javaTokens.PLUS, CodeBuilder::dadd);
	ret.put (javaTokens.MINUS, CodeBuilder::dsub);
	ret.put (javaTokens.MULTIPLY, CodeBuilder::dmul);
	ret.put (javaTokens.DIVIDE, CodeBuilder::ddiv);
	ret.put (javaTokens.REMAINDER, CodeBuilder::drem);
	return ret;
    }

    public static Map<Token, Consumer<CodeBuilder>> getFloatMap (JavaTokens javaTokens) {
	Map<Token, Consumer<CodeBuilder>> ret = new HashMap<> ();
	ret.put (javaTokens.PLUS, CodeBuilder::fadd);
	ret.put (javaTokens.MINUS, CodeBuilder::fsub);
	ret.put (javaTokens.MULTIPLY, CodeBuilder::fmul);
	ret.put (javaTokens.DIVIDE, CodeBuilder::fdiv);
	ret.put (javaTokens.REMAINDER, CodeBuilder::frem);
	return ret;
    }
}
