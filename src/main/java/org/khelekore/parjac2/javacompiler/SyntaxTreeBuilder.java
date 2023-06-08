package org.khelekore.parjac2.javacompiler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.CompilationException;
import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.javacompiler.syntaxtree.*;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.RuleNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class SyntaxTreeBuilder {
    private final CompilerDiagnosticCollector diagnostics;
    private final Java11Tokens java11Tokens;
    private final Grammar grammar;
    private final Map<String, ContextNodeBuilder> nodeBuilders;
    private final FlagConverter flagConverter;

    public SyntaxTreeBuilder (CompilerDiagnosticCollector diagnostics, Java11Tokens java11Tokens, Grammar grammar) {
	this.diagnostics = diagnostics;
	this.java11Tokens = java11Tokens;
	this.grammar = grammar;
	this.flagConverter = new FlagConverter (java11Tokens);
	nodeBuilders = new HashMap<> ();

	register ("GOAL", this::liftUp);

	// Productions from §3 (Lexical Structure)
	register ("Literal", this::liftUp);

	// Productions from §4 (Types, Values, and Variables)
	register ("Type", this::liftUp);
	register ("PrimitiveType", PrimitiveType::new);
	register ("NumericType", this::liftUp);
	register ("IntegralType", this::liftUp);
	register ("FloatingPointType", this::liftUp);
	register ("ReferenceType", this::liftUp);
	register ("ClassType", this::classType);
	register ("SimpleClassType", SimpleClassType::new);
	register ("TypeVariable", TypeVariable::new);
	register ("ArrayType", ArrayType::new);
	register ("Dims", Dims::new);
	register ("TypeParameter", TypeParameter::new);
	register ("TypeParameterModifier", this::liftUp);
	register ("TypeBound", TypeBound::new);
	register ("AdditionalBound", this::additionalBound);
	register ("TypeArguments", TypeArguments::new);
	register ("TypeArgumentList", TypeArgumentList::new);
	register ("TypeArgument", this::liftUp);
	register ("Wildcard", Wildcard::new);
	register ("WildcardBounds", WildcardBounds::new);

	// Productions from §6 (Names)
	register ("ModuleName", ModuleName::new);
	register ("PackageName", PackageName::new);
	register ("TypeName", TypeName::new);
	register ("ExpressionName", ExpressionName::new);
	register ("MethodName", this::liftUp);
	register ("PackageOrTypeName", PackageOrTypeName::new);
	register ("AmbiguousName", AmbiguousName::new);

	// Productions from §7 (Packages and Modules)
	register ("CompilationUnit", this::liftUp);
	register ("OrdinaryCompilationUnit", OrdinaryCompilationUnit::new);
	register ("ModularCompilationUnit", ModularCompilationUnit::new);
	register ("PackageDeclaration", PackageDeclaration::new);
	register ("PackageModifier", this::liftUp);
	register ("ImportDeclaration", this::liftUp);
	register ("SingleTypeImportDeclaration", SingleTypeImportDeclaration::new);
	register ("TypeImportOnDemandDeclaration", TypeImportOnDemandDeclaration::new);
	register ("SingleStaticImportDeclaration", SingleStaticImportDeclaration::new);
	register ("StaticImportOnDemandDeclaration", StaticImportOnDemandDeclaration::new);
	register ("TypeDeclaration", this::liftUp);
	register ("ModuleDeclaration", ModuleDeclaration::new);
	register ("ModuleDirective", this::moduleDirective);
	register ("RequiresModifier", this::liftUp);

	// Productions from §8 (Classes)
	register ("ClassDeclaration", this::liftUp);
	register ("NormalClassDeclaration", NormalClassDeclaration::new);
	register ("ClassModifier", this::liftUp);
	register ("TypeParameters", TypeParameters::new);
	register ("TypeParameterList", TypeParameterList::new);
	register ("Superclass", Superclass::new);
	register ("Superinterfaces", Superinterfaces::new);
	register ("InterfaceTypeList", InterfaceTypeList::new);
	register ("ClassBody", ClassBody::new);
	register ("ClassBodyDeclaration", this::liftUp);
	register ("ClassMemberDeclaration", this::liftUp);
	register ("FieldDeclaration", FieldDeclaration::new);
	register ("FieldModifier", this::liftUp);
	register ("VariableDeclaratorList", VariableDeclaratorList::new);
	register ("VariableDeclarator", VariableDeclarator::new);
	register ("VariableDeclaratorId", VariableDeclaratorId::new);
	register ("VariableInitializer", this::liftUp);
	register ("UnannType", this::liftUp);
	register ("UnannPrimitiveType", this::liftUp);
	register ("UnannReferenceType", this::liftUp);
	register ("UnannClassType", this::unannClassType);
	register ("UnannArrayType", UnannArrayType::new);
	register ("MethodDeclaration", MethodDeclaration::new);
	register ("MethodModifier", this::liftUp);
	register ("MethodHeader", MethodHeader::new);
	register ("Result", this::liftUp);
	register ("MethodDeclarator", MethodDeclarator::new);
	register ("ReceiverParameter", ReceiverParameter::new);
	register ("FormalParameterList", FormalParameterList::new);
	register ("FormalParameter", this::formalParameter);
	register ("VariableArityParameter", VariableArityParameter::new);
	register ("VariableModifier", this::liftUp);
	register ("Throws", Throws::new);
	register ("ExceptionTypeList", ExceptionTypeList::new);
	register ("ExceptionType", this::liftUp);
	register ("MethodBody", this::liftUp);
	register ("InstanceInitializer", this::liftUp);
	register ("StaticInitializer", StaticInitializer::new);
	register ("ConstructorDeclaration", ConstructorDeclaration::new);
	register ("ConstructorModifier", this::liftUp);
	register ("ConstructorDeclarator", ConstructorDeclarator::new);
	register ("SimpleTypeName",this::liftUp);
	register ("ConstructorBody", ConstructorBody::new);
	register ("ExplicitConstructorInvocation", ExplicitConstructorInvocation::new);
	register ("EnumDeclaration", EnumDeclaration::new);
	register ("EnumBody", EnumBody::new);
	register ("EnumConstantList", EnumConstantList::new);
	register ("EnumConstant", EnumConstant::new);
	register ("EnumConstantModifier", this::liftUp);
	register ("EnumBodyDeclarations", EnumBodyDeclarations::new);

	// Productions from §9 (Interfaces)
	register ("InterfaceDeclaration", this::liftUp);
	register ("NormalInterfaceDeclaration", NormalInterfaceDeclaration::new);
	register ("InterfaceModifier", this::liftUp);
	register ("ExtendsInterfaces", ExtendsInterfaces::new);
	register ("InterfaceBody", InterfaceBody::new);
	register ("InterfaceMemberDeclaration", this::liftUp);
	register ("ConstantDeclaration", ConstantDeclaration::new);
	register ("ConstantModifier", this::liftUp);
	register ("InterfaceMethodDeclaration", InterfaceMethodDeclaration::new);
	register ("InterfaceMethodModifier", this::liftUp);
	register ("AnnotationTypeDeclaration", AnnotationTypeDeclaration::new);
	register ("AnnotationTypeBody", AnnotationTypeBody::new);
	register ("AnnotationTypeMemberDeclaration", this::liftUp);
	register ("AnnotationTypeElementDeclaration", AnnotationTypeElementDeclaration::new);
	register ("AnnotationTypeElementModifier", this::liftUp);
	register ("DefaultValue", DefaultValue::new);
	register ("Annotation", this::liftUp);
	register ("NormalAnnotation", NormalAnnotation::new);
	register ("ElementValuePairList", ElementValuePairList::new);
	register ("ElementValuePair", ElementValuePair::new);
	register ("ElementValue", this::liftUp);
	register ("ElementValueArrayInitializer", ElementValueArrayInitializer::new);
	register ("ElementValueList", ElementValueList::new);
	register ("MarkerAnnotation", MarkerAnnotation::new);
	register ("SingleElementAnnotation", SingleElementAnnotation::new);

	// Productions from §10 (Arrays)
	register ("ArrayInitializer", ArrayInitializer::new);
	register ("VariableInitializerList", VariableInitializerList::new);

	// Productions from §14 (Blocks and Statements)
	register ("Block", Block::new);
	register ("BlockStatements", BlockStatements::new);
	register ("BlockStatement", this::liftUp);
	register ("LocalVariableDeclarationStatement", LocalVariableDeclarationStatement::new);
	register ("LocalVariableDeclaration", LocalVariableDeclaration::new);
	register ("LocalVariableType", this::liftUp);
	register ("Statement", this::liftUp);
	register ("StatementNoShortIf", this::liftUp);
	register ("StatementWithoutTrailingSubstatement", this::liftUp);
	register ("EmptyStatement", this::liftUp);
	register ("LabeledStatement", LabeledStatement::new);
	register ("LabeledStatementNoShortIf", LabeledStatement::new);
	register ("ExpressionStatement", ExpressionStatement::new);
	register ("StatementExpression", this::liftUp);
	register ("IfThenStatement", IfThenStatement::new);
	register ("IfThenElseStatement", IfThenStatement::new);
	register ("IfThenElseStatementNoShortIf", IfThenStatement::new);
	register ("AssertStatement", AssertStatement::new);
	register ("SwitchStatement", SwitchStatement::new);
	register ("SwitchBlock", SwitchBlock::new);
	register ("SwitchBlockStatementGroup", SwitchBlockStatementGroup::new);
	register ("SwitchLabels", SwitchLabels::new);
	register ("SwitchLabel", this::switchLabel);
	register ("WhileStatement", WhileStatement::new);
	register ("WhileStatementNoShortIf", WhileStatement::new);
	register ("DoStatement", DoStatement::new);
	register ("ForStatement", this::liftUp);
	register ("ForStatementNoShortIf", this::liftUp);
	register ("BasicForStatement", BasicForStatement::new);
	register ("BasicForStatementNoShortIf", BasicForStatement::new);
	register ("ForInit", this::liftUp);
	register ("ForUpdate", this::liftUp);
	register ("StatementExpressionList", StatementExpressionList::new);
	register ("EnhancedForStatement", EnhancedForStatement::new);
	register ("EnhancedForStatementNoShortIf", EnhancedForStatement::new);
	register ("BreakStatement", BreakStatement::new);
	register ("ContinueStatement", ContinueStatement::new);
	register ("ReturnStatement", ReturnStatement::new);
	register ("ThrowStatement", ThrowStatement::new);
	register ("SynchronizedStatement", SynchronizedStatement::new);
	register ("TryStatement", this::tryStatement);
	register ("Catches", Catches::new);
	register ("CatchClause", CatchClause::new);
	register ("CatchFormalParameter", CatchFormalParameter::new);
	register ("CatchType", CatchType::new);
	register ("Finally", Finally::new);
	register ("TryWithResourcesStatement", TryStatement::new);
	register ("ResourceSpecification", ResourceSpecification::new);
	register ("ResourceList", ResourceList::new);
	register ("Resource", this::resource);
	register ("VariableAccess", this::liftUp);

	// Productions from §15 (Expressions)
	register ("Primary", this::liftUp);
	register ("PrimaryNoNewArray", this::primaryNoNewArray);
	register ("ClassLiteral", ClassLiteral::new);
	register ("ClassInstanceCreationExpression", ClassInstanceCreationExpression::new);
	register ("UnqualifiedClassInstanceCreationExpression", UnqualifiedClassInstanceCreationExpression::new);
	register ("ClassOrInterfaceTypeToInstantiate", ClassOrInterfaceTypeToInstantiate::new);
	register ("TypeArgumentsOrDiamond", this::typeArgumentsOrDiamond);
	register ("FieldAccess", FieldAccess::new);
	register ("ArrayAccess", ArrayAccess::new);
	register ("MethodInvocation", this::methodInvocation);
	register ("UntypedMethodInvocation", UntypedMethodInvocation::new);
	register ("ArgumentList", ArgumentList::new);
	register ("MethodReference", this::methodReference);
	register ("ArrayCreationExpression", ArrayCreationExpression::new);
	register ("DimExprs", DimExprs::new);
	register ("DimExpr", DimExpr::new);
	register ("Expression", this::liftUp);
	register ("LambdaExpression", LambdaExpression::new);
	register ("LambdaParameters", this::lambdaParameters);
	register ("LambdaParameterList", this::lambdaParameterList);
	register ("LambdaParameter", this::lambdaParameter);
	register ("LambdaParameterType", this::liftUp);
	register ("LambdaBody", this::liftUp);
	register ("AssignmentExpression", this::liftUp);
	register ("Assignment", Assignment::new);
	register ("LeftHandSide", this::liftUp);
	register ("AssignmentOperator", this::liftUp);
	register ("ConditionalExpression", this::conditionalExpression);
	register ("ConditionalOrExpression", this::oneOrTwoParter);
	register ("ConditionalAndExpression", this::oneOrTwoParter);
	register ("InclusiveOrExpression", this::oneOrTwoParter);
	register ("ExclusiveOrExpression", this::oneOrTwoParter);
	register ("AndExpression", this::oneOrTwoParter);
	register ("EqualityExpression", this::oneOrTwoParter);
	register ("RelationalExpression", this::oneOrTwoParter);
	register ("ShiftExpression", this::oneOrTwoParter);
	register ("ShiftOp", this::shiftOp);
	register ("AdditiveExpression", this::oneOrTwoParter);
	register ("MultiplicativeExpression", this::oneOrTwoParter);
	register ("UnaryExpression", this::unaryExpression);
	register ("PreIncrementExpression", PreIncrementExpression::new);
	register ("PreDecrementExpression", PreDecrementExpression::new);
	register ("UnaryExpressionNotPlusMinus", this::unaryExpression);
	register ("PostfixExpression", this::liftUp);
	register ("PostIncrementExpression", PostIncrementExpression::new);
	register ("PostDecrementExpression", PostDecrementExpression::new);
	register ("CastExpression", CastExpression::new);
	register ("ConstantExpression", this::liftUp);
    }

    private final void register (String name, ContextNodeBuilder nb) {
	nodeBuilders.put (name, nb);
    }

    private final void register (String name, SimpleNodeBuilder snb) {
	nodeBuilders.put (name, (ctx, r, n, c) -> snb.build (r, n, c));
    }

    public ParseTreeNode build (DirAndPath dirAndPath, ParseTreeNode root) {
	Context ctx = new Context (java11Tokens, grammar, diagnostics, dirAndPath, flagConverter);
	return build (ctx, root);
    }

    private ParseTreeNode build (Context ctx, ParseTreeNode root) {
	List<ParseTreeNode> children = root.getChildren ();
	List<ParseTreeNode> convertedChildren = new ArrayList<> (children.size ());
	for (ParseTreeNode c : children)
	    convertedChildren.add (build (ctx, c));
	ParseTreeNode n = convert (ctx, root, convertedChildren);
	return n;
    }

    private ParseTreeNode convert (Context ctx, ParseTreeNode node, List<ParseTreeNode> children) {
	if (node.isToken ()) {
	    return node;
	}
	RuleNode rn = (RuleNode)node;
	Rule rule = rn.getRule ();

	if (rule.getName ().startsWith ("_ZOM")) {
	    node = buildZom (rule, node, children);
	} else {
	    ContextNodeBuilder nb = nodeBuilders.get (rule.getName ());
	    if (nb != null) {
		node = nb.build (ctx, rule, rn, children);
	    } else {
		// Children may be changed so need to update to new node
		node = new RuleNode (rule, children);
	    }
	}
	return node;
    }

    private interface ContextNodeBuilder {
	ParseTreeNode build (Context ctx, Rule rule, ParseTreeNode input, List<ParseTreeNode> children);
    }

    private interface SimpleNodeBuilder {
	ParseTreeNode build (Rule rule, ParseTreeNode input, List<ParseTreeNode> children);
    }

    private ParseTreeNode classType (Context ctx, Rule rule, ParseTreeNode ct, List<ParseTreeNode> children) {
	if (rule.size () == 1) {
	    List<SimpleClassType> ls = new ArrayList<> ();
	    ls.add ((SimpleClassType)children.get (0));
	    return new ClassType (ct.getPosition (), ls);
	}
	// ClassType . SimpleClassType
	ClassType cct = (ClassType)children.get (0);
	SimpleClassType csct = (SimpleClassType)children.get (2);
	List<SimpleClassType> ls = cct.getTypes ();
	ls.add (csct);
	return new ClassType (ct.getPosition (), ls);
    }

    private ParseTreeNode additionalBound (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	return children.get (1);
    }

    private ParseTreeNode unannClassType (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 3) {
	    UnannClassType uct = (UnannClassType)children.get (0);
	    SimpleClassType sct = (SimpleClassType)children.get (2);
	    uct.add (sct);
	    return uct;
	}
	TypeIdentifier i = (TypeIdentifier)children.get (0);
	TypeArguments tas = children.size () > 1 ? (TypeArguments)children.get (1) : null;
	SimpleClassType sct = new SimpleClassType (n.getPosition (), i.getValue (), tas);
	return new UnannClassType (sct);
    }

    private ParseTreeNode formalParameter (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () > 1)
	    return new FormalParameter (rule, n, children);
	return children.get (0); // lift it up
    }

    private ParseTreeNode moduleDirective (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	int ruleId = rule.get (0);
	if (ruleId == java11Tokens.REQUIRES.getId ()) {
	    return new RequiresDirective (rule, n, children);
	} else if (ruleId == java11Tokens.EXPORTS.getId ()) {
	    return new ExportsDirective (rule, n, children);
	} else if (ruleId == java11Tokens.OPENS.getId ()) {
	    return new OpensDirective (rule, n, children);
	} else if (ruleId == java11Tokens.USES.getId ()) {
	    return new UsesDirective (rule, n, children);
	} else if (ruleId == java11Tokens.PROVIDES.getId ()) {
	    return new ProvidesDirective (rule, n, children);
	} else {
	    ctx.error (n.getPosition (), "Unhandled rule: %d, %s", ruleId, rule.toReadableString (grammar));
	    throw new CompilationException ();
	}
    }

    private SyntaxTreeNode switchLabel (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 2)
	    return new DefaultLabel (n.getPosition ());
	return new CaseLabel (n.getPosition (), children.get (1));
    }

    private ParseTreeNode tryStatement (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return children.get (0);
	return new TryStatement (rule, n, children);
    }

    private ParseTreeNode resource (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return new SimpleResource (children.get (0));
	return new Resource (rule, n, children);
    }

    private ParseTreeNode primaryNoNewArray (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.get (0) == java11Tokens.THIS.getId ())
	    return new ThisPrimary (n);
	if (rule.size () == 1)
	    return children.get (0);
	if (rule.get (0) == java11Tokens.LEFT_PARENTHESIS.getId ())
	    return children.get (1);
	return new DottedThis (children.get (0));
    }

    private ParseTreeNode typeArgumentsOrDiamond (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () > 1)
	    return new Diamond (n.getPosition ());
	return children.get (0);
    }

    private SyntaxTreeNode methodInvocation (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	ParsePosition pos = children.get (0).getPosition ();
	if (rule.size () == 1) {
	    return new MethodInvocation (pos, null, false, null, (UntypedMethodInvocation)children.get (0));
	}
	ParseTreeNode on = null;
	boolean isSuper = false;

	int i = 0;
	if (rule.get (0) == java11Tokens.SUPER.getId ()) {  // super.<types>mi
	    isSuper = true;
	    i += 2;
	} else if (rule.size () > 4) {  // Type.super.<types>mi
	    on = children.get (0);
	    isSuper = true;
	    i += 4;
	} else { // Name.<types>mi or Primary.<types>mi
	    on = children.get (0);
	    i += 2;
	}
	TypeArguments types = rule.size () > i + 1 ? (TypeArguments)children.get (i) : null;
	UntypedMethodInvocation mi = (UntypedMethodInvocation)children.get (children.size () - 1);
	return new MethodInvocation (pos, on, isSuper, types, mi);
    }

    private ParseTreeNode methodReference (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.get (rule.size () - 1) == java11Tokens.IDENTIFIER.getId ()) {
	    if (rule.size () > 4)
		return new SuperMethodReference (rule, n, children);
	    return new NormalMethodReference (rule, n, children);
	} else { // NEW
	    return new ConstructorMethodReference (rule, n, children);
	}
    }

    private ParseTreeNode lambdaParameters (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	switch (rule.size ()) {
	case 1: // x ->
	    // TODO: rewrite to (x) -> so we can remove one case
	    return new IdentifierLambdaParameters (n, ((Identifier)children.get (0)).getValue ());
	case 2: // () ->
	    return new ListOfLambdaParameters (n, null);
	case 3: // (<something>) ->
	    return new ListOfLambdaParameters (n, (LambdaParameterList<?>)children.get (1));
	default:
	    throw new IllegalStateException ("Unhandled rule length: " + rule.toReadableString (grammar));
	}
    }

    private ParseTreeNode lambdaParameterList (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.get (0) == java11Tokens.IDENTIFIER.getId ())
	    return LambdaParameterList.forIdentifier (rule, n, children, i -> ((Identifier)i).getValue ());
	return LambdaParameterList.forParameters (rule, n, children, c -> (LambdaParameter)c);
    }

    private ParseTreeNode lambdaParameter (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)  // VariableArityParameter
	    return new VarArgLambdaParameter (n.getPosition (), (VariableArityParameter)children.get (0));
	int i = 0;
	List<ParseTreeNode> modifiers = Collections.emptyList ();
	if (rule.size () > 2)
	    modifiers = ((Multiple)children.get (i++)).get ();
	ParseTreeNode type = children.get (i++);
	VariableDeclaratorId vid = (VariableDeclaratorId)children.get (i);
	return new FullTypeLambdaParameter (n.getPosition (), modifiers, type, vid);
    }

    private ParseTreeNode shiftOp (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	switch (rule.size ()) {
	case 1:
	    return children.get (0);
	case 2:
	    return new TokenNode (java11Tokens.RIGHT_SHIFT, n.getPosition ());
	case 3:
	    return new TokenNode (java11Tokens.RIGHT_SHIFT_UNSIGNED, n.getPosition ());
	}
	ctx.error (n.getPosition (), "%s got unexpected size %d, children: %d",
		   rule.toReadableString (grammar), rule.size (), children);
	    throw new CompilationException ();
    }

    private ParseTreeNode conditionalExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return children.get (0);
	return new Ternary (rule, n, children);
    }

    private ParseTreeNode oneOrTwoParter (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return children.get (0);
	return new TwoPartExpression (rule, n, children);
    }

    private ParseTreeNode unaryExpression (Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return children.get (0);
	return new UnaryExpression (rule, n, children);
    }

    private ParseTreeNode liftUp (Context ctx, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	return children.get (0);
    }

    private ParseTreeNode buildZom (Rule rule, ParseTreeNode node, List<ParseTreeNode> children) {
	if (rule.size () > 1 && rule.get (0) == rule.getGroupId ()) {
	    Multiple z = (Multiple)children.get (0);
	    z.addAll (children.subList (1, children.size ()));
	    return z;
	} else {
	    return new Multiple (node.getPosition (), rule.get (0), rule.getName (),
				 new ArrayList<> (children));
	}
    }
}
