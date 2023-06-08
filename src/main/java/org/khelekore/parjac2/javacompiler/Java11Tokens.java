package org.khelekore.parjac2.javacompiler;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.Token;

public class Java11Tokens {

    // 3.4. Line Terminators
    public final Token LF;
    public final Token CR;
    public final Token CRLF;

    // 3.5. Input Elements and Tokens
    public final Token SUB;

    // 3.6. White Space
    public final Token WHITESPACE;

    // 3.7. Comments
    public final Token TRADITIONAL_COMMENT;
    public final Token END_OF_LINE_COMMENT;

    // 3.8. Identifiers
    public final Token IDENTIFIER;
    public final Token TYPE_IDENTIFIER;
    public final Token VAR;

    // 3.9. Keywords
    public final Token ABSTRACT;
    public final Token ASSERT;
    public final Token BOOLEAN;
    public final Token BREAK;
    public final Token BYTE;
    public final Token CASE;
    public final Token CATCH;
    public final Token CHAR;
    public final Token CLASS;
    public final Token CONST;
    public final Token CONTINUE;
    public final Token DEFAULT;
    public final Token DO;
    public final Token DOUBLE;
    public final Token ELSE;
    public final Token ENUM;
    public final Token EXTENDS;
    public final Token FINAL;
    public final Token FINALLY;
    public final Token FLOAT;
    public final Token FOR;
    public final Token GOTO;
    public final Token IF;
    public final Token IMPLEMENTS;
    public final Token IMPORT;
    public final Token INSTANCEOF;
    public final Token INT;
    public final Token INTERFACE;
    public final Token LONG;
    public final Token NATIVE;
    public final Token NEW;
    public final Token PACKAGE;
    public final Token PRIVATE;
    public final Token PROTECTED;
    public final Token PUBLIC;
    public final Token RETURN;
    public final Token SHORT;
    public final Token STATIC;
    public final Token STRICTFP;
    public final Token SUPER;
    public final Token SWITCH;
    public final Token SYNCHRONIZED;
    public final Token THIS;
    public final Token THROW;
    public final Token THROWS;
    public final Token TRANSIENT;
    public final Token TRY;
    public final Token UNDERSCORE;
    public final Token VOID;
    public final Token VOLATILE;
    public final Token WHILE;

    // restricted keywords
    public final Token EXPORTS;
    public final Token MODULE;
    public final Token OPEN;
    public final Token OPENS;
    public final Token PROVIDES;
    public final Token REQUIRES;
    public final Token TO;
    public final Token TRANSITIVE;
    public final Token USES;
    public final Token WITH;

    // 3.10. Literals
    public final Token INT_LITERAL;
    public final Token LONG_LITERAL;
    public final Token FLOAT_LITERAL;
    public final Token DOUBLE_LITERAL;
    public final Token CHARACTER_LITERAL;
    public final Token STRING_LITERAL;
    public final Token NULL;
    public final Token TRUE;
    public final Token FALSE;

    // 3.11. Separators
    public final Token LEFT_PARENTHESIS;
    public final Token RIGHT_PARENTHESIS;
    public final Token LEFT_CURLY;
    public final Token RIGHT_CURLY;
    public final Token LEFT_BRACKET;
    public final Token RIGHT_BRACKET;
    public final Token SEMICOLON;
    public final Token COMMA;
    public final Token DOT;
    public final Token ELLIPSIS;
    public final Token AT;
    public final Token DOUBLE_COLON;

    // 3.12. Operators
    public final Token EQUAL;
    public final Token GT;
    public final Token LT;
    public final Token NOT;
    public final Token TILDE;
    public final Token QUESTIONMARK;
    public final Token COLON;
    public final Token ARROW;
    public final Token DOUBLE_EQUAL;
    public final Token GE;
    public final Token LE;
    public final Token NOT_EQUAL;
    public final Token LOGICAL_AND;
    public final Token LOGICAL_OR;
    public final Token INCREMENT;
    public final Token DECREMENT;
    public final Token PLUS;
    public final Token MINUS;
    public final Token MULTIPLY;
    public final Token DIVIDE;
    public final Token AND;
    public final Token OR;
    public final Token XOR;
    public final Token REMAINDER;
    public final Token LEFT_SHIFT;
    public final Token RIGHT_SHIFT;
    public final Token RIGHT_SHIFT_UNSIGNED;
    public final Token PLUS_EQUAL;
    public final Token MINUS_EQUAL;
    public final Token MULTIPLY_EQUAL;
    public final Token DIVIDE_EQUAL;
    public final Token BIT_AND_EQUAL;
    public final Token BIT_OR_EQUAL;
    public final Token BIT_XOR_EQUAL;
    public final Token REMAINDER_EQUAL;
    public final Token LEFT_SHIFT_EQUAL;
    public final Token RIGHT_SHIFT_EQUAL;
    public final Token RIGHT_SHIFT_UNSIGNED_EQUAL;

    private final BitSet whitespaces = new BitSet ();
    private final BitSet comments = new BitSet ();
    private final BitSet keywords = new BitSet ();
    private final BitSet restrictedKeywords = new BitSet ();
    private final BitSet literals = new BitSet ();
    private final BitSet separators = new BitSet ();
    private final BitSet operators = new BitSet ();

    private final Map<String, Token> nameToKeyword = new HashMap<> ();
    private final Map<String, Token> nameToRestrictedKeyword = new HashMap<> ();

    public Java11Tokens (Grammar grammar) {

	// 3.4. Line Terminators
	LF = grammar.getToken ("LF");          // \n
	CR = grammar.getToken ("CR");          // \r
	CRLF = grammar.getToken ("CRLF");      // \r\n

	// 3.5. Input Elements and Tokens
	SUB = grammar.getToken ("SUB"); // \u001a, only allowed at end of input and should be ignored

	// 3.6. White Space
	WHITESPACE = grammar.getToken ("whitespace");  // ' ', \t, \f

	// 3.7. Comments
	TRADITIONAL_COMMENT = grammar.getToken ("TraditionalComment");
	END_OF_LINE_COMMENT = grammar.getToken ("EndOfLineComment");

	// 3.8. Identifiers
	IDENTIFIER = grammar.getToken ("Identifier");
	TYPE_IDENTIFIER = grammar.getToken ("TypeIdentifier");
	VAR = grammar.getToken ("var");

	// 3.9. Keywords
	ABSTRACT = grammar.getToken ("abstract");
	ASSERT = grammar.getToken ("assert");
	BOOLEAN = grammar.getToken ("boolean");
	BREAK = grammar.getToken ("break");
	BYTE = grammar.getToken ("byte");
	CASE = grammar.getToken ("case");
	CATCH = grammar.getToken ("catch");
	CHAR = grammar.getToken ("char");
	CLASS = grammar.getToken ("class");
	CONST = grammar.getToken ("const");
	CONTINUE = grammar.getToken ("continue");
	DEFAULT = grammar.getToken ("default");
	DO = grammar.getToken ("do");
	DOUBLE = grammar.getToken ("double");
	ELSE = grammar.getToken ("else");
	ENUM = grammar.getToken ("enum");
	EXTENDS = grammar.getToken ("extends");
	FINAL = grammar.getToken ("final");
	FINALLY = grammar.getToken ("finally");
	FLOAT = grammar.getToken ("float");
	FOR = grammar.getToken ("for");
	GOTO = grammar.getToken ("goto");
	IF = grammar.getToken ("if");
	IMPLEMENTS = grammar.getToken ("implements");
	IMPORT = grammar.getToken ("import");
	INSTANCEOF = grammar.getToken ("instanceof");
	INT = grammar.getToken ("int");
	INTERFACE = grammar.getToken ("interface");
	LONG = grammar.getToken ("long");
	NATIVE = grammar.getToken ("native");
	NEW = grammar.getToken ("new");
	PACKAGE = grammar.getToken ("package");
	PRIVATE = grammar.getToken ("private");
	PROTECTED = grammar.getToken ("protected");
	PUBLIC = grammar.getToken ("public");
	RETURN = grammar.getToken ("return");
	SHORT = grammar.getToken ("short");
	STATIC = grammar.getToken ("static");
	STRICTFP = grammar.getToken ("strictfp");
	SUPER = grammar.getToken ("super");
	SWITCH = grammar.getToken ("switch");
	SYNCHRONIZED = grammar.getToken ("synchronized");
	THIS = grammar.getToken ("this");
	THROW = grammar.getToken ("throw");
	THROWS = grammar.getToken ("throws");
	TRANSIENT = grammar.getToken ("transient");
	TRY = grammar.getToken ("try");
	UNDERSCORE = grammar.getToken ("_");
	VOID = grammar.getToken ("void");
	VOLATILE = grammar.getToken ("volatile");
	WHILE = grammar.getToken ("while");

	// restricted keywords for module directives
	EXPORTS = grammar.getToken ("exports");
	MODULE = grammar.getToken ("module");
	OPEN = grammar.getToken ("open");
	OPENS = grammar.getToken ("opens");
	PROVIDES = grammar.getToken ("provides");
	REQUIRES = grammar.getToken ("requires");
	TO = grammar.getToken ("to");
	TRANSITIVE = grammar.getToken ("transitive");
	USES = grammar.getToken ("uses");
	WITH = grammar.getToken ("with");

	// 3.10. Literals
	INT_LITERAL = grammar.getToken  ("int_literal");
	LONG_LITERAL = grammar.getToken  ("long_literal");
	FLOAT_LITERAL = grammar.getToken  ("float_literal");
	DOUBLE_LITERAL = grammar.getToken  ("double_literal");
	CHARACTER_LITERAL = grammar.getToken  ("character_literal");
	STRING_LITERAL = grammar.getToken  ("string_literal");
	NULL = grammar.getToken  ("null");
	TRUE = grammar.getToken  ("true");
	FALSE = grammar.getToken  ("false");

	// 3.11. Separators
	LEFT_PARENTHESIS = grammar.getToken ("(");
	RIGHT_PARENTHESIS = grammar.getToken  (")");
	LEFT_CURLY = grammar.getToken  ("{");
	RIGHT_CURLY = grammar.getToken  ("}");
	LEFT_BRACKET = grammar.getToken  ("[");
	RIGHT_BRACKET = grammar.getToken  ("]");
	SEMICOLON = grammar.getToken (";");
	COMMA = grammar.getToken (",");
	DOT = grammar.getToken (".");
	ELLIPSIS = grammar.getToken ("...");
	AT = grammar.getToken ("@");
	DOUBLE_COLON = grammar.getToken ("::");

	// 3.12. Operators
	EQUAL = grammar.getToken ("=");
	GT = grammar.getToken (">");
	LT = grammar.getToken ("<");
	NOT = grammar.getToken ("!");
	TILDE = grammar.getToken ("~");
	QUESTIONMARK = grammar.getToken ("?");
	COLON = grammar.getToken (":");
	ARROW = grammar.getToken ("->");
	DOUBLE_EQUAL = grammar.getToken ("==");
	GE = grammar.getToken (">=");
	LE = grammar.getToken ("<=");
	NOT_EQUAL = grammar.getToken ("!=");
	LOGICAL_AND = grammar.getToken ("&&");
	LOGICAL_OR = grammar.getToken ("||");
	INCREMENT = grammar.getToken ("++");
	DECREMENT = grammar.getToken ("--");
	PLUS = grammar.getToken ("+");
	MINUS = grammar.getToken ("-");
	MULTIPLY = grammar.getToken ("*");
	DIVIDE = grammar.getToken ("/");
	AND = grammar.getToken ("&");
	OR = grammar.getToken ("|");
	XOR = grammar.getToken ("^");
	REMAINDER = grammar.getToken ("%");
	LEFT_SHIFT = grammar.getToken ("<<");
	RIGHT_SHIFT = grammar.getToken (">>");
	RIGHT_SHIFT_UNSIGNED = grammar.getToken (">>>");
	PLUS_EQUAL = grammar.getToken ("+=");
	MINUS_EQUAL = grammar.getToken ("-=");
	MULTIPLY_EQUAL = grammar.getToken ("*=");
	DIVIDE_EQUAL = grammar.getToken ("/=");
	BIT_AND_EQUAL = grammar.getToken ("&=");
	BIT_OR_EQUAL = grammar.getToken ("|=");
	BIT_XOR_EQUAL = grammar.getToken ("^=");
	REMAINDER_EQUAL = grammar.getToken ("%=");
	LEFT_SHIFT_EQUAL = grammar.getToken ("<<=");
	RIGHT_SHIFT_EQUAL = grammar.getToken (">>=");
	RIGHT_SHIFT_UNSIGNED_EQUAL = grammar.getToken (">>>=");

	store (whitespaces, WHITESPACE, LF, CR, CRLF); // line terminators are also whitespace
	store (comments, TRADITIONAL_COMMENT, END_OF_LINE_COMMENT);
	store (keywords, ABSTRACT, ASSERT, BOOLEAN, BREAK, BYTE, CASE, CATCH, CHAR, CLASS,
	       CONST, CONTINUE, DEFAULT, DO, DOUBLE, ELSE, ENUM, EXTENDS, FINAL, FINALLY,
	       FLOAT, FOR, GOTO, IF, IMPLEMENTS, IMPORT, INSTANCEOF, INT, INTERFACE, LONG,
	       NATIVE, NEW, PACKAGE, PRIVATE, PROTECTED, PUBLIC, RETURN, SHORT, STATIC, STRICTFP,
	       SUPER, SWITCH, SYNCHRONIZED, THIS, THROW, THROWS, TRANSIENT, TRY, UNDERSCORE, VOID,
	       VOLATILE, WHILE);
	store (restrictedKeywords, EXPORTS, MODULE, OPEN, OPENS, PROVIDES, REQUIRES,
	       TO, TRANSITIVE, USES, WITH);
	store (literals, INT_LITERAL, LONG_LITERAL, FLOAT_LITERAL, DOUBLE_LITERAL,
	       CHARACTER_LITERAL, STRING_LITERAL, NULL, TRUE, FALSE);
	store (separators, LEFT_PARENTHESIS, RIGHT_PARENTHESIS, LEFT_CURLY, RIGHT_CURLY,
	       LEFT_BRACKET, RIGHT_BRACKET, SEMICOLON, COMMA, DOT, ELLIPSIS, AT, DOUBLE_COLON);
	store (operators, EQUAL, GT, LT, NOT, TILDE, QUESTIONMARK, COLON, ARROW, DOUBLE_EQUAL,
	       GE, LE, NOT_EQUAL, LOGICAL_AND, LOGICAL_OR, INCREMENT, DECREMENT, PLUS, MINUS,
	       MULTIPLY, DIVIDE, AND, OR, XOR, REMAINDER, LEFT_SHIFT, RIGHT_SHIFT,
	       RIGHT_SHIFT_UNSIGNED, PLUS_EQUAL, MINUS_EQUAL, MULTIPLY_EQUAL, DIVIDE_EQUAL,
	       BIT_AND_EQUAL, BIT_OR_EQUAL, BIT_XOR_EQUAL, REMAINDER_EQUAL, LEFT_SHIFT_EQUAL,
	       RIGHT_SHIFT_EQUAL, RIGHT_SHIFT_UNSIGNED_EQUAL);

	keywords.stream ().mapToObj (i -> grammar.getToken (i))
	    .forEach (t -> nameToKeyword.put (t.getName (), t));
	nameToKeyword.put (NULL.getName (), NULL);
	nameToKeyword.put (TRUE.getName (), TRUE);
	nameToKeyword.put (FALSE.getName (), FALSE);

	restrictedKeywords.stream ().mapToObj (i -> grammar.getToken (i))
	    .forEach (t -> nameToRestrictedKeyword.put (t.getName (), t));
	nameToRestrictedKeyword.put (VAR.getName (), VAR);
    }

    private final void store (BitSet store, Token... tokens) {
	for (Token token : tokens)
	    store.set (token.getId ());
    }

    public boolean isWhitespace (Token t) {
	return whitespaces.get (t.getId ());
    }

    public boolean isComment (Token t) {
	return comments.get (t.getId ());
    }

    public boolean isKeyword (Token t) {
	return keywords.get (t.getId ());
    }

    public boolean isLiteral (Token t) {
	return literals.get (t.getId ());
    }

    public boolean isSeparator (Token t) {
	return separators.get (t.getId ());
    }

    public boolean isOperator (Token t) {
	return operators.get (t.getId ());
    }

    /** Check if a String is an identifier or a keyword or the null, true or false litera. */
    public Token getKeywordFromIdentifier (String id) {
	return nameToKeyword.get (id);
    }

    public Token getRestrictedKeyWordFromIdentifier (String id) {
	return nameToRestrictedKeyword.get (id);
    }
}
