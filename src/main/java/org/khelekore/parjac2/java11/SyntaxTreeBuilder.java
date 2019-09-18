package org.khelekore.parjac2.java11;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.khelekore.parjac2.CompilerDiagnosticCollector;
import org.khelekore.parjac2.parser.Grammar;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.RuleNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class SyntaxTreeBuilder {
    private final Java11Tokens java11Tokens;
    private final Grammar grammar;
    private final CompilerDiagnosticCollector diagnostics;
    private final Map<String, NodeBuilder> nodeBuilders;

    public SyntaxTreeBuilder (Java11Tokens java11Tokens, Grammar grammar, CompilerDiagnosticCollector diagnostics) {
	this.java11Tokens = java11Tokens;
	this.grammar = grammar;
	this.diagnostics = diagnostics;
	nodeBuilders = new HashMap<> ();

	register ("GOAL", this::liftUp);

	// Productions from §3 (Lexical Structure)

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
/*
NormalInterfaceDeclaration:
InterfaceModifier:
ExtendsInterfaces:
InterfaceBody:
InterfaceMemberDeclaration:
ConstantDeclaration:
*/
	register ("ConstantModifier", this::liftUp);
/*
InterfaceMethodDeclaration:
*/
	register ("InterfaceMethodModifier", this::liftUp);
/*
AnnotationTypeDeclaration:
AnnotationTypeBody:
*/
	register ("AnnotationTypeMemberDeclaration", this::liftUp);
/*
AnnotationTypeElementDeclaration:
*/
	register ("AnnotationTypeElementModifier", this::liftUp);
	register ("DefaultValue", DefaultValue::new);
	register ("Annotation", this::liftUp);
	register ("NormalAnnotation", NormalAnnotation::new);
/*
ElementValuePairList:
ElementValuePair:
*/
	register ("ElementValue", this::liftUp);
/*
ElementValueArrayInitializer:
ElementValueList:
*/
	register ("MarkerAnnotation", MarkerAnnotation::new);
/*
SingleElementAnnotation:
*/
	// Productions from §10 (Arrays)
	register ("ArrayInitializer", ArrayInitializer::new);
	register ("VariableInitializerList", VariableInitializerList::new);

	// Productions from §14 (Blocks and Statements)
	register ("Block", Block::new);
	register ("BlockStatements", BlockStatements::new);
	register ("BlockStatement", this::liftUp);
/*
LocalVariableDeclarationStatement:
LocalVariableDeclaration:
*/
	register ("LocalVariableType", this::liftUp);
	register ("Statement", this::liftUp);
	register ("StatementNoShortIf", this::liftUp);
	register ("StatementWithoutTrailingSubstatement", this::liftUp);
	register ("EmptyStatement", this::liftUp);
	register ("LabeledStatement", LabeledStatement::new);
	register ("LabeledStatementNoShortIf", LabeledStatement::new);
	register ("ExpressionStatement", ExpressionStatement::new);
	register ("StatementExpression", this::liftUp);
/*
IfThenStatement:
IfThenElseStatement:
IfThenElseStatementNoShortIf:
AssertStatement:
SwitchStatement:
SwitchBlock:
SwitchLabels:
SwitchLabel:
WhileStatement:
WhileStatementNoShortIf:
DoStatement:
*/
	register ("ForStatement", this::liftUp);
	register ("ForStatementNoShortIf", this::liftUp);
/*
BasicForStatement:
BasicForStatementNoShortIf:
*/
	register ("ForInit", this::liftUp);
	register ("ForUpdate", this::liftUp);
/*
StatementExpressionList:
EnhancedForStatement:
EnhancedForStatementNoShortIf:
BreakStatement:
ContinueStatement:
ReturnStatement:
ThrowStatement:
SynchronizedStatement:
TryStatement:
Catches:
CatchClause:
CatchFormalParameter:
CatchType:
Finally:
TryWithResourcesStatement:
ResourceSpecification:
ResourceList:
Resource:
VariableAccess:
*/
	// Productions from §15 (Expressions)
	register ("Primary", this::liftUp);
/*
PrimaryNoNewArray:
ClassLiteral:
ClassInstanceCreationExpression:
UnqualifiedClassInstanceCreationExpression:
ClassOrInterfaceTypeToInstantiate:
TypeArgumentsOrDiamond:
FieldAccess:
ArrayAccess:
MethodInvocation:
UntypedMethodInvocation:
*/
	register ("ArgumentList", ArgumentList::new);
/*
MethodReference:
ArrayCreationExpression:
DimExprs:
DimExpr:
*/
	register ("Expression", this::liftUp);
/*
LambdaExpression:
LambdaParameters:
LambdaParameterList:
LambdaParameter:
*/
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

    private final void register (String name, NodeBuilder nb) {
	nodeBuilders.put (name, nb);
    }

    public ParseTreeNode build (Path path, ParseTreeNode root) {
	List<ParseTreeNode> children = root.getChildren ();
	List<ParseTreeNode> convertedChildren =
	    children.stream ().map (c -> build (path, c)).collect (Collectors.toList ());
	ParseTreeNode n = convert (path, root, convertedChildren);
	return n;
    }

    private ParseTreeNode convert (Path path, ParseTreeNode node, List<ParseTreeNode> children) {
	if (node.isToken ()) {
	    return node;
	}
	RuleNode rn = (RuleNode)node;
	Rule rule = rn.getRule ();

	if (rule.getName ().startsWith ("_ZOM")) {
	    node = buildZom (rule, node, children);
	} else {
	    NodeBuilder nb = nodeBuilders.get (rule.getName ());
	    if (nb != null) {
		node = nb.build (path, rule, rn, children);
	    } else {
		// Children may be changed so need to update to new node
		node = new RuleNode (rule, children);
	    }
	}
	return node;
    }

    private interface NodeBuilder {
	ParseTreeNode build (Path path, Rule rule, ParseTreeNode input, List<ParseTreeNode> children);
    }

    private class PrimitiveType extends ComplexTreeNode {
	private List<Annotation> annotations;
	private Token type;

	public PrimitiveType (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    if (rule.size () > 1) {
		annotations = ((ZOMEntry)children.get (0)).get ();
		type = ((TokenNode)children.get (1)).getToken ();
	    } else {
		annotations = Collections.emptyList ();
		type = ((TokenNode)children.get (0)).getToken ();
	    }
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    sb.append (type);
	    return sb.toString ();
	}
    }

    private ParseTreeNode classType (Path path, Rule rule, ParseTreeNode ct, List<ParseTreeNode> children) {
	if (rule.size () == 1) {
	    List<SimpleClassType> ls = new ArrayList<> ();
	    ls.add ((SimpleClassType)children.get (0));
	    return new ClassType (ct.getPosition (), ls);
	}
	// ClassType . SimpleClassType
	ClassType cct = (ClassType)children.get (0);
	SimpleClassType csct = (SimpleClassType)children.get (2);
	List<SimpleClassType> ls = cct.types;
	ls.add (csct);
	return new ClassType (ct.getPosition (), ls);
    }

    private class ClassType extends ComplexTreeNode {
	private final List<SimpleClassType> types;

	public ClassType (ParsePosition pos, List<SimpleClassType> types) {
	    super (pos);
	    this.types = types;
	}

	@Override public Object getValue() {
	    return types.toString ();
	}
    }

    private class SimpleClassType extends ComplexTreeNode {
	private final List<Annotation> annotations;
	private final String id;
	private final TypeArguments typeArguments;

	public SimpleClassType (Path path, Rule rule, ParseTreeNode sct, List<ParseTreeNode> children) {
	    super (sct.getPosition ());
	    int i = 0;
	    ParseTreeNode n = children.get (i++);
	    if (n instanceof ZOMEntry) {
		annotations = ((ZOMEntry)n).get ();
		n = children.get (i++);
	    } else {
		annotations = Collections.emptyList ();
	    }
	    id = ((Identifier)n).getValue ();
	    typeArguments = rule.size () > i ? (TypeArguments)children.get (i) : null;
	}

	public SimpleClassType (ParsePosition pos, List<Annotation> annotations, String id, TypeArguments tas) {
	    super (pos);
	    this.annotations = annotations;
	    this.id = id;
	    this.typeArguments = tas;
	}

	@Override public Object getValue () {
	    String a = annotations.stream ().map (x -> x.toString ()).collect (Collectors.joining (" "));
	    return a + id + (typeArguments != null ? typeArguments : "");
	}
    }

    private class TypeVariable extends ComplexTreeNode {
	private List<Annotation> annotations;
	private String id;

	public TypeVariable (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    if (rule.size () > 1) {
		annotations = ((ZOMEntry)children.get (0)).get ();
		id = ((Identifier)children.get (1)).getValue ();
	    } else {
		annotations = Collections.emptyList ();
		id = ((Identifier)children.get (0)).getValue ();
	    }
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    sb.append (id);
	    return sb.toString ();
	}
    }

    private class ArrayType extends ComplexTreeNode {
	private ParseTreeNode type;
	private Dims dims;
	public ArrayType (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    type = children.get (0); // primitive or class type
	    dims = (Dims)children.get (1);
	}

	@Override public Object getValue () {
	    return type.toString () + dims;
	}
    }

    private class Dims extends ComplexTreeNode {
	// one list of annotations per dim
	List<List<ParseTreeNode>> annotations;
	public Dims (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    annotations = new ArrayList<> ();
	    int i = 0;
	    if (children.get (0) instanceof ZOMEntry)
		annotations.add (((ZOMEntry)children.get (i++)).get ());
	    else
		annotations.add (Collections.emptyList ());
	    if (rule.size () > i + 2) {
		ZOMEntry z = (ZOMEntry)children.get (i + 2);
		for (int j = 0; j < z.nodes.size (); j += 2) {
		    if (z.nodes.get (j) instanceof ZOMEntry)
			annotations.add (((ZOMEntry)z.nodes.get (j++)).get ());
		    else
			annotations.add (Collections.emptyList ());
		}
	    }
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    for (List<ParseTreeNode> ls : annotations) {
		if (!ls.isEmpty ())
		    sb.append (ls.toString ());
		sb.append ("[]");
	    }
	    return sb.toString ();
	}
    }

    private class TypeParameter extends ComplexTreeNode {
	private List<Annotation> annotations;
	private String id;
	private TypeBound bound;

	public TypeParameter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (i) instanceof ZOMEntry) {
		annotations = ((ZOMEntry)children.get (i++)).get ();
	    } else {
		annotations = Collections.emptyList ();
	    }
	    id = ((Identifier)children.get (i++)).getValue ();
	    bound = rule.size () > i ? (TypeBound)children.get (i) : null;
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    sb.append (id);
	    if (bound != null)
		sb.append (" ").append (bound);
	    return sb.toString ();
	}
    }

    private class TypeBound extends ComplexTreeNode {
	private ClassType base;
	private List<ClassType> additionalBounds;

	private TypeBound (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    base = (ClassType)children.get (1);
	    if (rule.size () > 2) {
		additionalBounds = new ArrayList<> ();
		ZOMEntry z = (ZOMEntry)children.get (2);
		z.nodes.forEach (c -> additionalBounds.add ((ClassType)c));
	    } else {
		additionalBounds = Collections.emptyList ();
	    }
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("extends ").append (base);
	    for (ClassType c : additionalBounds)
		sb.append (" & ").append (c);
	    return sb.toString ();
	}
    }

    private ParseTreeNode additionalBound (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	return children.get (1);
    }

    private class TypeArguments extends ComplexTreeNode {
	private final List<ParseTreeNode> typeArgumentList;
	public TypeArguments (Path path, Rule rule, ParseTreeNode sct, List<ParseTreeNode> children) {
	    super (sct.getPosition ());
	    // '<' TypeArgumentList '>'
	    typeArgumentList = ((TypeArgumentList)children.get (1)).get ();
	}

	@Override public Object getValue () {
	    return "<" + typeArgumentList + ">";
	}
    }

    private class TypeArgumentList extends CommaListBase {
	public TypeArgumentList (Path path, Rule r, ParseTreeNode tal, List<ParseTreeNode> children) {
	    super (path, r, tal, children);
	}
    }

    private class Wildcard extends ComplexTreeNode {
	private final List<Annotation> annotations;
	private final WildcardBounds bounds;

	private Wildcard (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (0) instanceof ZOMEntry)
		annotations = ((ZOMEntry)children.get (i++)).get ();
	    else
		annotations = Collections.emptyList ();
	    bounds = (rule.size () > i + 1) ? (WildcardBounds)children.get (i + 1) : null;
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    sb.append ("?");
	    if (bounds != null)
		sb.append (" ").append (bounds);
	    return sb.toString ();
	}
    }

    private class WildcardBounds extends ComplexTreeNode {
	private Token type;
	private ParseTreeNode referenceType;

	private WildcardBounds (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    type = ((TokenNode)children.get (0)).getToken ();
	    referenceType = children.get (1);
	}

	@Override public Object getValue() {
 	    return type.getName () + " " + referenceType;
	}
    }

    private class ModuleName extends DottedName {
	public ModuleName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class PackageName extends DottedName {
	public PackageName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class TypeName extends DottedName {
	public TypeName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class ExpressionName extends DottedName {
	public ExpressionName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class PackageOrTypeName extends DottedName {
	public PackageOrTypeName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class AmbiguousName extends DottedName {
	public AmbiguousName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class DottedName extends ComplexTreeNode {
	private final List<String> nameParts;
	public DottedName (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    if (rule.size () == 1) {
		nameParts = new ArrayList<> ();
		nameParts.add (((Identifier)children.get (0)).getValue ());
	    } else {
		DottedName pot = (DottedName)children.get (0);
		nameParts = pot.nameParts;
		nameParts.add (((Identifier)children.get (2)).getValue ());
	    }
	}

	@Override public Object getValue() {
	    return dotted (nameParts);
	}
    }

    private class OrdinaryCompilationUnit extends ComplexTreeNode {
	private PackageDeclaration packageDeclarataion;
	private List<ImportDeclaration> imports;
	private List<TypeDeclaration> types;

	public OrdinaryCompilationUnit (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (rule.size () > i && rule.get (i) == grammar.getRuleGroupId ("PackageDeclaration"))
		packageDeclarataion = (PackageDeclaration)children.get (i++);
	    else
		packageDeclarataion = null;

	    if (rule.size () > i && isZomImports (children.get (i)))
		imports = ((ZOMEntry)children.get (i++)).<ImportDeclaration>get ();
	    else
		imports = Collections.emptyList ();

	    if (rule.size () > i)
		types = ((ZOMEntry)children.get (i++)).<TypeDeclaration>get ();
	    else
		types = Collections.emptyList ();
	}

	private boolean isZomImports (ParseTreeNode n) {
	    if (n instanceof ZOMEntry) {
		ZOMEntry z = (ZOMEntry)n;
		if (z.getInternalGroupId () == grammar.getRuleGroupId ("ImportDeclaration"))
		    return true;
	    }
	    return false;
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (packageDeclarataion != null)
		sb.append (packageDeclarataion).append ("\n");
	    for (ImportDeclaration id : imports)
		sb.append (id).append ("\n");
	    for (TypeDeclaration type : types)
		sb.append (type).append ("\n");
	    return sb.toString ();
	}
    }

    private class ModularCompilationUnit extends ComplexTreeNode {
	private List<ImportDeclaration> imports;
	private ModuleDeclaration moduleDeclaration;

	public ModularCompilationUnit (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    imports = (rule.size () > 1) ? ((ZOMEntry)children.get (i++)).get () : Collections.emptyList ();
	    moduleDeclaration = (ModuleDeclaration)children.get (i++);
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!imports.isEmpty ()) {
		for (ImportDeclaration i : imports)
		    sb.append (i).append ("\n");
	    }
	    sb.append (moduleDeclaration);
	    return sb.toString ();
	}
    }

    private class PackageDeclaration extends ComplexTreeNode {
	private final List<Annotation> annotations;
	private final List<String> nameParts;

	public PackageDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (rule.get (i) == java11Tokens.PACKAGE.getId ()) {
		annotations = Collections.emptyList ();
	    } else {
		ZOMEntry z = (ZOMEntry)children.get (i++);
		annotations = z.get ();
	    }
	    nameParts = new ArrayList<> ();
	    String firstPart = ((Identifier)children.get (i + 1)).getValue ();
	    nameParts.add (firstPart);
	    if (children.size () > i + 1) {
		ZOMEntry z = (ZOMEntry)children.get (i + 2);
		for (int j = 1; j < z.nodes.size (); j += 2)
		    nameParts.add (((Identifier)z.nodes.get (j)).getValue ());
	    }
	}

	@Override public Object getValue () {
	    return annotations + (annotations.isEmpty () ? "" : " ") + "package " + dotted (nameParts) + ";\n";
	}
    }

    private class SingleTypeImportDeclaration extends ImportDeclaration {
	private TypeName typename;
	public SingleTypeImportDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (1);
	}

	@Override public Object getValue () {
	    return "import " + typename + ";";
	}
    }

    private class TypeImportOnDemandDeclaration extends ImportDeclaration {
	private final PackageOrTypeName typename;
	public TypeImportOnDemandDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (PackageOrTypeName)children.get (1);
	}

	@Override public Object getValue () {
	    return "import " + typename + ".*;";
	}
    }

    private class SingleStaticImportDeclaration extends ImportDeclaration {
	private TypeName typename;
	private String id;
	public SingleStaticImportDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (2);
	    this.id = ((Identifier)children.get (4)).getValue ();
	}

	@Override public Object getValue () {
	    return "import static " + typename + "." + id + ";";
	}
    }

    private class StaticImportOnDemandDeclaration extends ImportDeclaration {
	private TypeName typename;
	public StaticImportOnDemandDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (2);
	}

	@Override public Object getValue () {
	    return "import static " + typename + ".*;";
	}
    }

    private abstract class ImportDeclaration extends ComplexTreeNode {
	public ImportDeclaration (ParsePosition pos) {
	    super (pos);
	}
    }

    private class NormalClassDeclaration extends TypeDeclaration {
	private List<ParseTreeNode> modifiers;
	private String id;
	private TypeParameters typeParameters;
	private Superclass superClass;
	private Superinterfaces superInterfaces;
	private ClassBody classBody;

	public NormalClassDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (i) instanceof ZOMEntry)
		modifiers = ((ZOMEntry)children.get (i++)).get ();
	    else
		modifiers = Collections.emptyList ();
	    i++; // 'class'
	    id = ((TypeIdentifier)children.get (i++)).getValue ();
	    if (children.get (i) instanceof TypeParameters)
		typeParameters = (TypeParameters)children.get (i++);
	    if (children.get (i) instanceof Superclass)
		superClass = (Superclass)children.get (i++);
	    if (children.get (i) instanceof Superinterfaces)
		superInterfaces = (Superinterfaces)children.get (i++);
	    classBody = (ClassBody)children.get (i++);
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append ("class ").append (id);
	    if (typeParameters != null)
		sb.append (typeParameters).append (" ");
	    if (superClass != null)
		sb.append (superClass).append (" ");
	    if (superInterfaces != null)
		sb.append (superInterfaces).append (" ");
	    sb.append (classBody);
	    return sb.toString ();
	}
    }

    private class TypeParameters extends ComplexTreeNode {
	private final TypeParameterList list;

	public TypeParameters (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.list = (TypeParameterList)children.get (1);
	}

	@Override public Object getValue () {
	    return "<" + list + ">";
	}
    }

    private class TypeParameterList extends CommaListBase {
	public TypeParameterList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class Superclass extends ComplexTreeNode {
	private ClassType type;
	public Superclass (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.type = (ClassType)children.get (1);
	}

	@Override public Object getValue () {
	    return "extends " + type;
	}
    }

    private class Superinterfaces extends ComplexTreeNode {
	private List<ClassType> types;
	public Superinterfaces (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    InterfaceTypeList l = (InterfaceTypeList)children.get (1);
	    types = l.types;
	}

	@Override public Object getValue () {
	    return "implements " + types.stream ().map (x -> x.toString ()).collect (Collectors.joining (", "));
	}
    }

    private class InterfaceTypeList extends ComplexTreeNode {
	private List<ClassType> types;
	public InterfaceTypeList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    types = new ArrayList<> ();
	    types.add ((ClassType)children.get (0));
	    if (children.size () > 1) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int i = 1; i < z.nodes.size (); i += 2)
		    types.add ((ClassType)z.nodes.get (i));
	    }
	}

	@Override public Object getValue () {
	    return types;
	}
    }

    private class ClassBody extends ComplexTreeNode {
	private List<ParseTreeNode> declarations;
	public ClassBody (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    declarations = (rule.size () > 2) ? ((ZOMEntry)children.get (1)).get () : Collections.emptyList ();
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (" {\n");
	    for (ParseTreeNode d : declarations)
		sb.append (d).append ("\n");
	    sb.append ("}\n");
	    return sb.toString ();
	}
    }

    private abstract class ClassBodyDeclaration extends ComplexTreeNode {
	public ClassBodyDeclaration (ParsePosition pos) {
	    super (pos);
	}
    }

    private class FieldDeclaration extends ClassBodyDeclaration {
	private List<ParseTreeNode> modifiers;
	private ParseTreeNode type;
	private VariableDeclaratorList list;

	public FieldDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    modifiers = (rule.size () > 3) ? ((ZOMEntry)children.get (i++)).get () : Collections.emptyList ();
	    type = children.get (i++);
	    list = (VariableDeclaratorList)children.get (i++);
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append (type).append (" ").append (list).append (";");
	    return sb.toString ();
	}
    }

    private class VariableDeclaratorList extends ComplexTreeNode {
	private List<VariableDeclarator> declarators;

	public VariableDeclaratorList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    declarators = new ArrayList<> ();
	    declarators.add ((VariableDeclarator)children.get (0));
	    if (rule.size () > 1) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int i = 1; i < z.nodes.size (); i += 2)
		    declarators.add ((VariableDeclarator)z.nodes.get (i));
	    }
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (declarators.get (0));
	    for (int i = 1; i < declarators.size (); i++)
		sb.append (", ").append (declarators.get (i));
	    return sb.toString ();
	}
    }

    private class VariableDeclarator extends ComplexTreeNode {
	private VariableDeclaratorId id;
	private ParseTreeNode initializer;
	public VariableDeclarator (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    id = (VariableDeclaratorId)children.get (0);
	    if (rule.size () > 1)
		initializer = children.get (2);
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (id);
	    if (initializer != null)
		sb.append (" = ").append (initializer);
	    return sb.toString ();
	}
    }

    private class VariableDeclaratorId extends ComplexTreeNode {
	private String id;
	private Dims dims;

	public VariableDeclaratorId (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    id = ((Identifier)children.get (0)).getValue ();
	    if (rule.size () > 1)
		dims = (Dims)children.get (1);
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (id);
	    if (dims != null)
		sb.append (dims);
	    return sb.toString ();
	}
    }

    private ParseTreeNode unannClassType (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 3) {
	    UnannClassType uct = (UnannClassType)children.get (0);
	    SimpleClassType sct = (SimpleClassType)children.get (2);
	    uct.types.add (sct);
	    return uct;
	} else {
	    TypeIdentifier i = (TypeIdentifier)children.get (0);
	    TypeArguments tas = children.size () > 1 ? (TypeArguments)children.get (1) : null;
	    SimpleClassType sct =
		new SimpleClassType (n.getPosition (), Collections.emptyList (), i.getValue (), tas);
	    return new UnannClassType (sct);
	}
    }

    private class UnannClassType extends ComplexTreeNode {
	private List<SimpleClassType> types;

	public UnannClassType (SimpleClassType sct) {
	    super (sct.getPosition ());
	    this.types = new ArrayList<> ();
	    types.add (sct);
	}

	@Override public Object getValue() {
	    return types.toString ();
	}
    }

    private class UnannArrayType extends ComplexTreeNode {
	private ParseTreeNode type;
	private Dims dims;
	public UnannArrayType (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    type = children.get (0);
	    dims = (Dims)children.get (1);
	}

	@Override public Object getValue() {
	    return type.toString () + dims;
	}
    }

    private class MethodDeclaration extends ClassBodyDeclaration {
	private List<ParseTreeNode> modifiers;
	private MethodHeader header;
	private ParseTreeNode body; // Block or ;
	public MethodDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    modifiers = (rule.size () > 2) ? ((ZOMEntry)children.get (i++)).get () : Collections.emptyList ();
	    header = (MethodHeader)children.get (i++);
	    body = children.get (i);
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append (header).append (" ").append (body);
	    return sb.toString ();
	}
    }

    private class MethodHeader extends ComplexTreeNode {
	private TypeParameters types;
	private List<ParseTreeNode> annotations;
	private ParseTreeNode result;
	private MethodDeclarator methodDeclarator;
	private Throws t;

	public MethodHeader (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    annotations = Collections.emptyList ();
	    if (rule.get (0) == grammar.getRuleGroupId ("TypeParameters")) {
		types = (TypeParameters)children.get (i++);
		if (children.get (i) instanceof ZOMEntry)
		    annotations = ((ZOMEntry)children.get (i++)).get ();
	    }
	    result = children.get (i++);
	    methodDeclarator = (MethodDeclarator)children.get (i++);
	    if (rule.size () > i)
		t = (Throws)children.get (i);
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (types != null)
		sb.append (types).append (" ");
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    sb.append (result).append (" ").append (methodDeclarator);
	    if (t != null)
		sb.append (" ").append (t);
	    return sb.toString ();
	}
    }

    private class MethodDeclarator extends ComplexTreeNode {
	private String id;
	private ReceiverParameter rp;
	private FormalParameterList params;
	private Dims dims;

	public MethodDeclarator (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    id = ((Identifier)children.get (0)).getValue ();
	    int i = 2;
	    if (rule.get (i) == grammar.getRuleGroupId ("ReceiverParameter")) {
		rp = (ReceiverParameter)children.get (i);
		i += 2;
	    }
	    if (rule.get (i) == grammar.getRuleGroupId ("FormalParameterList"))
		params = (FormalParameterList)children.get (i++);
	    i++;
	    if (rule.size () > i)
		dims = (Dims)children.get (i);
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (id).append ("(");
	    if (rp != null)
		sb.append (rp).append (", ");
	    if (params != null)
		sb.append (params);
	    sb.append (")");
	    if (dims != null)
		sb.append (dims);
	    return sb.toString ();
	}
    }

    private class ReceiverParameter extends ComplexTreeNode {
	private List<Annotation> annotations;
	private ParseTreeNode type;
	private String id;

	public ReceiverParameter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (i) instanceof ZOMEntry)
		annotations = ((ZOMEntry)children.get (i++)).get ();
	    else
		annotations = Collections.emptyList ();
	    type = children.get (i++);
	    if (rule.get (i) != java11Tokens.THIS.getId ())
		id = ((Identifier)children.get (i)).getValue ();
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    sb.append (type).append (" ");
	    if (id != null)
		sb.append (id).append (".");
	    sb.append ("this");
	    return sb.toString ();
	}
    }

    private class FormalParameterList extends ComplexTreeNode {
	List<FormalParameterBase> params;
	public FormalParameterList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    params = new ArrayList<> ();
	    params.add ((FormalParameterBase)children.get (0));
	    if (rule.size () > 1) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int i = 1; i < z.nodes.size (); i += 2)
		    params.add ((FormalParameterBase)z.nodes.get (i));
	    }
	}

	@Override public Object getValue() {
	    return params;
	}
    }

    private ParseTreeNode formalParameter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () > 1)
	    return new FormalParameter (path, rule, n, children);
	return children.get (0); // lift it up
    }

    private class FormalParameter extends FormalParameterBase {
	private final List<ParseTreeNode> modifiers;
	private final ParseTreeNode type;
	private final VariableDeclaratorId var;

	// since we have the formalParameter method we do not have to care about VariableArityParameter
	public FormalParameter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    modifiers = (rule.size () > 2) ? ((ZOMEntry)children.get (i++)).get () : Collections.emptyList ();
	    type = children.get (i++);
	    var = (VariableDeclaratorId)children.get (i);
	}
	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append (type).append (" ").append (var);
	    return sb.toString ();
	}
    }

    private class VariableArityParameter extends FormalParameterBase {
	private final List<ParseTreeNode> modifiers;
	private final ParseTreeNode type;
	private final List<Annotation> annotations;
	private final String id;
	public VariableArityParameter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    modifiers = (children.get (i) instanceof ZOMEntry) ?
		((ZOMEntry)children.get (i++)).get () : Collections.emptyList ();
	    type = children.get (i++);
	    annotations = (children.get (i) instanceof ZOMEntry) ?
		((ZOMEntry)children.get (i++)).get () : Collections.emptyList ();
	    i++; // ...
	    id = ((Identifier)children.get (i)).getValue ();
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append (type).append (" ");
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    sb.append ("...").append (id);
	    return sb.toString ();
	}
    }

    private abstract class FormalParameterBase extends ComplexTreeNode {
	public FormalParameterBase (ParsePosition pos) {
	    super (pos);
	}
    }

    private class Throws extends ComplexTreeNode {
	private ExceptionTypeList exceptionTypeList;
	public Throws (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    exceptionTypeList = (ExceptionTypeList)children.get (1);
	}

	@Override public Object getValue() {
	    return "throws " + exceptionTypeList;
	}
    }

    private class ExceptionTypeList extends ComplexTreeNode {
	private List<ClassType> types;
	public ExceptionTypeList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    types = new ArrayList<> ();
	    types.add ((ClassType)children.get (0));
	    if (rule.size () > 1) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int i = 1; i < z.nodes.size (); i += 2)
		    types.add ((ClassType)z.nodes.get (i));
	    }
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (types.get (0));
	    for (int i = 1; i < types.size (); i++)
		sb.append (", ").append (types.get (i));
	    return sb.toString ();
	}
    }

    private class StaticInitializer extends ClassBodyDeclaration {
	private final Block block;
	public StaticInitializer (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    block = (Block)children.get (1);
	}

	@Override public Object getValue () {
	    return "static " + block;
	}
    }

    private class ConstructorDeclaration extends ClassBodyDeclaration {
	private final List<ParseTreeNode> modifiers;
	private final ConstructorDeclarator declarator;
	private final Throws t;
	private final ConstructorBody body;
	private ConstructorDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (i) instanceof ZOMEntry)
		modifiers = ((ZOMEntry)children.get (i++)).get ();
	    else
		modifiers = Collections.emptyList ();
	    declarator = (ConstructorDeclarator)children.get (i++);
	    t = (rule.size () > i + 1) ? (Throws)children.get (i++) : null;
	    body = (ConstructorBody)children.get (i);
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append (declarator).append (" ");
	    if (t != null)
		sb.append (t).append (" ");
	    sb.append (body);
	    return sb.toString ();
	}
    }

    private class ConstructorDeclarator extends ComplexTreeNode {
	private TypeParameters types;
	private String id;
	private ReceiverParameter rp;
	private FormalParameterList params;
	private ConstructorDeclarator (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (rule.get (i) == grammar.getRuleGroupId ("TypeParameters")) 
		types = (TypeParameters)children.get (i++);
	    id = ((Identifier)children.get (i++)).getValue ();
	    if (rule.get (i) == grammar.getRuleGroupId ("ReceiverParameter")) {
		rp = (ReceiverParameter)children.get (i);
		i += 2;
	    }
	    if (rule.get (i) == grammar.getRuleGroupId ("FormalParameterList"))
		params = (FormalParameterList)children.get (i++);
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (types != null)
		sb.append (types).append (" ");
	    sb.append (id).append (" (");
	    if (rp != null) {
		sb.append (rp);
		if (params != null)
		    sb.append (", ");
	    }
	    if (params != null)
		sb.append (params);
	    sb.append (")");
	    return sb.toString ();
	}
    }

    private class ConstructorBody extends ComplexTreeNode {
	private ExplicitConstructorInvocation eci;
	private BlockStatements statements;
	private ConstructorBody (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 1;
	    if (rule.get (i) == grammar.getRuleGroupId ("ExplicitConstructorInvocation"))
		eci = (ExplicitConstructorInvocation)children.get (i++);
	    if (rule.get (i) == grammar.getRuleGroupId ("BlockStatements"))
		statements = (BlockStatements)children.get (i);
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("{\n");
	    if (eci != null)
		sb.append (eci);
	    if (statements != null)
		sb.append (statements);
	    sb.append ("\n}");
	    return sb.toString ();
	}
    }

    private class ExplicitConstructorInvocation extends ComplexTreeNode {
	private final ParseTreeNode type; // ExpresisonName or Primary
	private final TypeArguments types;
	private final Token where; // this or super
	private final ArgumentList argumentList;
	private ExplicitConstructorInvocation (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (rule.get (i) == grammar.getRuleGroupId ("ExpressionName") ||
		rule.get (i) == grammar.getRuleGroupId ("Primary")) {
		type = children.get (i);
		i += 2;
	    } else {
		type = null;
	    }
	    if (rule.get (i) == grammar.getRuleGroupId ("TypeArguments"))
		types = (TypeArguments)children.get (i++);
	    else
		types = null;
	    where = ((TokenNode)children.get (i++)).getToken ();
	    i++; // (
	    if (rule.get (i) == grammar.getRuleGroupId ("ArgumentList"))
		argumentList = (ArgumentList)children.get (i);
	    else
		argumentList = null;
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (type != null)
		sb.append (type).append (".");
	    if (types != null)
		sb.append (types);
	    sb.append (where);
	    sb.append ("(");
	    if (argumentList != null)
		sb.append (argumentList);
	    sb.append (");");
	    return sb.toString ();
	}
    }

    private class EnumDeclaration extends TypeDeclaration {
	private final List<ParseTreeNode> modifiers;
	private final String id;
	private final Superinterfaces supers;
	private final EnumBody body;
	public EnumDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (0) instanceof ZOMEntry) {
		modifiers = ((ZOMEntry)children.get (i++)).get ();
	    } else {
		modifiers = Collections.emptyList ();
	    }
	    i++;
	    id = ((Identifier)children.get (i++)).getValue ();
	    if (rule.size () > i + 2) {
		supers = (Superinterfaces)children.get (i++);
	    } else {
		supers = null;
	    }
	    body = (EnumBody)children.get (i);
	}
	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append ("enum ").append (id).append (" ");
	    if (supers != null)
		sb.append (supers).append (" ");
	    sb.append (body);
	    return sb.toString ();
	}
    }

    private class EnumBody extends ComplexTreeNode {
	private final EnumConstantList constants;
	private final EnumBodyDeclarations declarations;
	public EnumBody (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 1;
	    if (children.get (i) instanceof EnumConstantList) {
		constants = (EnumConstantList)children.get (i++);
	    } else {
		constants = null;
	    }
	    if (rule.get (i) == java11Tokens.COMMA.getId ())
		i++;
	    if (children.get (i) instanceof EnumBodyDeclarations) {
		declarations = (EnumBodyDeclarations)children.get (i);
	    } else {
		declarations = null;
	    }
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("{");
	    if (constants != null)
		sb.append (constants);
	    if (declarations != null)
		sb.append (declarations);
	    sb.append ("}");
	    return sb.toString ();
	}
    }

    private class EnumConstantList extends ComplexTreeNode {
	private List<EnumConstant> constants;
	public EnumConstantList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    if (rule.size () == 1) {
		constants = List.of ((EnumConstant)children.get (0));
	    } else {
		constants = new ArrayList<> ();
		constants.add ((EnumConstant)children.get (0));
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int j = 1; j < z.nodes.size (); j += 2)
		    constants.add ((EnumConstant)z.nodes.get (j));
	    }
	}

	@Override public Object getValue () {
	    return constants.stream ().map (ec -> ec.toString ()).collect (Collectors.joining (", "));
	}
    }

    private class EnumConstant extends ComplexTreeNode {
	private final List<ParseTreeNode> modifiers;
	private final String id;
	private final ArgumentList args;
	private final ClassBody body;
	public EnumConstant (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (i) instanceof ZOMEntry) {
		modifiers = ((ZOMEntry)children.get (i++)).get ();
	    } else {
		modifiers = Collections.emptyList ();
	    }
	    id = ((Identifier)children.get (i++)).getValue ();
	    if (rule.size () > i && rule.get (i++) == java11Tokens.LEFT_PARENTHESIS.getId ()) {
		if (rule.size () > i && rule.get (i) != java11Tokens.RIGHT_PARENTHESIS.getId ()) {
		    args = (ArgumentList)children.get (i++);
		} else {
		    args = null;
		}
		i++; // ')'
	    } else {
		args = null;
	    }
	    body = (rule.size () > i) ? (ClassBody)children.get (i) : null;
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append (id);
	    if (args != null)
		sb.append ("(").append (args).append (")");
	    if (body != null)
		sb.append (body);
	    return sb.toString ();
	}
    }

    private class EnumBodyDeclarations extends ComplexTreeNode {
	private List<ClassBodyDeclaration> body;
	public EnumBodyDeclarations(Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    if (rule.size () > 1) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		body = z.get ();
	    }
	}

	@Override public Object getValue () {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (";");
	    if (body != null)
		sb.append (body);
	    return sb.toString ();
	}
    }

    private abstract class TypeDeclaration extends ComplexTreeNode {
	public TypeDeclaration (ParsePosition pos) {
	    super (pos);
	}
    }

    private class ModuleDeclaration extends ComplexTreeNode {
	private List<Annotation> annotations;
	private boolean isOpen;
	private List<String> identifiers;
	private List<ModuleDirective> directives;

	public ModuleDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 0;
	    if (children.get (i) instanceof ZOMEntry)
		annotations = ((ZOMEntry)children.get (i++)).get ();
	    else
		annotations = Collections.emptyList ();

	    isOpen = (rule.get (i) == java11Tokens.OPEN.getId ());
	    if (isOpen)
		i++;

	    i++; // 'module'
	    identifiers = new ArrayList<> ();
	    identifiers.add (((Identifier)children.get (i++)).getValue ());
	    if (children.get (i) instanceof ZOMEntry) {
		ZOMEntry z = (ZOMEntry)children.get (i++);
		for (int j = 1; j < z.nodes.size (); j += 2)
		    identifiers.add (((Identifier)z.nodes.get (j)).getValue ());
	    }
	    i++; // '{'
	    if (children.get (i) instanceof ZOMEntry)
		directives = ((ZOMEntry)children.get (i++)).get ();
	    else
		directives = Collections.emptyList ();

	    i++; // '}'
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    if (!annotations.isEmpty ())
		sb.append (annotations).append (" ");
	    if (isOpen)
		sb.append ("open ");
	    sb.append ("module ");
	    sb.append (identifiers.get (0));
	    for (int i = 1; i < identifiers.size (); i++)
		sb.append (".").append (identifiers.get (i));
	    sb.append ("{\n");
	    for (ModuleDirective d : directives)
		sb.append (d).append ("\n");
	    sb.append ("}\n");
	    return sb.toString ();
	}
    }

    private ParseTreeNode moduleDirective (Path path, Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	if (r.get (0) == java11Tokens.REQUIRES.getId ()) {
	    return new RequiresDirective (r, n, children);
	} else if (r.get (0) == java11Tokens.EXPORTS.getId ()) {
	    return new ExportsDirective (r, n, children);
	} else if (r.get (0) == java11Tokens.OPENS.getId ()) {
	    return new OpensDirective (r, n, children);
	} else if (r.get (0) == java11Tokens.USES.getId ()) {
	    return new UsesDirective (r, n, children);
	} else if (r.get (0) == java11Tokens.PROVIDES.getId ()) {
	    return new ProvidesDirective (r, n, children);
	} else {
	    throw new IllegalArgumentException ("Type: " + r.get (0) + ", " + r.toReadableString (grammar));
	}
    }

    private class RequiresDirective extends ModuleDirective {
	private final List<ParseTreeNode> modifiers;
	private final ModuleName moduleName;

	public RequiresDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    int i = 1;
	    modifiers = (r.size () > 3) ? ((ZOMEntry)children.get (i++)).get () : Collections.emptyList ();
	    moduleName = (ModuleName)children.get (i);
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("requires ");
	    if (!modifiers.isEmpty ())
		sb.append (modifiers).append (" ");
	    sb.append (moduleName).append (";");
	    return sb.toString ();
	}
    }

    private class ExportsDirective extends PackageNameTo<PackageName, ModuleName> {
	public ExportsDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (r, n, children);
	}

	@Override protected String getType () {
	    return "exports";
	}

	@Override protected String getThing () {
	    return "to";
	}
    }

    private class OpensDirective extends PackageNameTo<PackageName, ModuleName> {
	public OpensDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (r, n, children);
	}

	@Override protected String getType () {
	    return "opens";
	}

	@Override protected String getThing () {
	    return "to";
	}
    }

    private class UsesDirective extends ModuleDirective {
	private TypeName typeName;

	public UsesDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    typeName = (TypeName)children.get (1);
	}

	@Override public Object getValue() {
	    return "uses " + typeName + ";";
	}
    }

    private class ProvidesDirective extends PackageNameTo<TypeName, TypeName> {
	public ProvidesDirective (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (r, n, children);
	}

	@Override protected String getType () {
	    return "provides";
	}

	@Override protected String getThing () {
	    return "with";
	}
    }

    private abstract class PackageNameTo<T extends DottedName, S extends DottedName> extends ModuleDirective {
	private T packageName;
	private List<S> exportedTo;

	@SuppressWarnings("unchecked")
	public PackageNameTo (Rule r, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    packageName = (T)children.get (1);
	    if (r.size () > 3) {
		exportedTo = new ArrayList<> ();
		exportedTo.add ((S)children.get (3));
		if (r.size () > 5) {
		    ZOMEntry z = (ZOMEntry)children.get (4);
		    for (int i = 1; i < z.nodes.size (); i += 2)
			exportedTo.add ((S)z.nodes.get (i));
		}
	    }
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append (getType ()).append (" ").append (packageName);
	    if (exportedTo != null) {
		sb.append (" ").append (getThing ()).append (" " ).append (exportedTo.get (0));
		for (int i = 1; i < exportedTo.size (); i++)
		    sb.append (", " + exportedTo.get (i));
	    }
	    sb.append (";");
	    return sb.toString ();
	}

	protected abstract String getType ();
	protected abstract String getThing ();
    }

    private abstract class ModuleDirective extends ComplexTreeNode {
	public ModuleDirective (ParsePosition pos) {
	    super (pos);
	}
    }

    private class DefaultValue extends ComplexTreeNode {
	private ParseTreeNode value;
	public DefaultValue (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.value = children.get (1);
	}

	@Override public Object getValue() {
	    return "default " + value;
	}
    }

    private abstract class Annotation extends ComplexTreeNode {
	public Annotation (ParsePosition pos) {
	    super (pos);
	}
    }

    private class NormalAnnotation extends Annotation {
	private TypeName typename;
	private ParseTreeNode elementValuePairList;
	public NormalAnnotation (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (1);
	    elementValuePairList = rule.size () > 4 ? children.get (3) : null;
	}

	@Override public Object getValue() {
	    return "@" + typename + "(" + (elementValuePairList != null ? elementValuePairList : "") + ")";
	}
    }

    private class MarkerAnnotation extends Annotation {
	private TypeName typename;
	public MarkerAnnotation (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (1);
	}

	@Override public Object getValue() {
	    return "@" + typename;
	}
    }

    private class ArrayInitializer extends ComplexTreeNode {
	private final  VariableInitializerList variableList;
	public ArrayInitializer (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    if (rule.get (1) == grammar.getRuleGroupId ("VariableInitializerList"))
		variableList = (VariableInitializerList)children.get (1);
	    else
		variableList = null;
	}
	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("{");
	    if (variableList != null)
		sb.append (variableList);
	    sb.append ("}");
	    return sb.toString ();
	}
    }

    private class VariableInitializerList extends ComplexTreeNode {
	private List<ParseTreeNode> variableInitializers;
	public VariableInitializerList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    variableInitializers = new ArrayList<> ();
	    variableInitializers.add (children.get (0));
	    if (rule.size () > 2) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int j = 1; j < z.nodes.size (); j += 2)
		    variableInitializers.add (z.nodes.get (j));
	    }
	}

	@Override public Object getValue() {
	    return variableInitializers;
	}
    }

    private class Block extends ComplexTreeNode {
	private final BlockStatements statements;
	public Block (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    statements = (rule.size () > 2) ? (BlockStatements)children.get (1) : null;
	}

	@Override public Object getValue() {
	    return "{\n" + statements + "\n}";
	}
    }

    private class BlockStatements extends ComplexTreeNode {
	private List<ParseTreeNode> statements;
	public BlockStatements (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    statements = new ArrayList<> ();
	    statements.add (children.get (0));
	    if (rule.size () > 1)
		statements.addAll (((ZOMEntry)children.get (1)).get ());
	}

	@Override public Object getValue() {
	    return statements;
	}
    }

    private class LabeledStatement extends ComplexTreeNode {
	private String id;
	private ParseTreeNode statement;
	public LabeledStatement (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    id = ((Identifier)children.get (0)).getValue ();
	    statement = children.get (2);
	}

	@Override public Object getValue() {
	    return id + ":" + statement;
	}
    }

    private class ExpressionStatement extends ComplexTreeNode {
	private ParseTreeNode statementExpression;
	public ExpressionStatement (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    statementExpression = children.get (0);
	}

	@Override public Object getValue () {
	    return statementExpression + ";";
	}
    }

    private class ArgumentList extends CommaListBase {
	public ArgumentList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (path, rule, n, children);
	}
    }

    private class Assignment extends ComplexTreeNode {
	private ParseTreeNode left;
	private Token operator;
	private ParseTreeNode right;

	public Assignment (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    left = children.get (0);
	    operator = ((TokenNode)children.get (1)).getToken ();
	    right = children.get (2);
	}

	@Override public Object getValue() {
	    return left + " " + operator + " " + right;
	}
    }

    private ParseTreeNode shiftOp (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	switch (rule.size ()) {
	case 1:
	    return children.get (0);
	case 2:
	    return new TokenNode (java11Tokens.RIGHT_SHIFT, n.getPosition ());
	case 3:
	    return new TokenNode (java11Tokens.RIGHT_SHIFT_UNSIGNED, n.getPosition ());
	}
	throw new IllegalArgumentException (path + ": " + rule.toReadableString (grammar) +
					    " got unexpected size: " + rule.size () +
					    ", children: " + children);
    }

    private ParseTreeNode conditionalExpression (Path path, Rule rule, ParseTreeNode n,
						 List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return children.get (0);
	return new Ternary (path, rule, n, children);
    }

    private class Ternary extends ComplexTreeNode {
	private ParseTreeNode test;
	private ParseTreeNode thenPart;
	private ParseTreeNode elsePart;
	public Ternary (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    test = children.get (0);
	    thenPart = children.get (2);
	    elsePart = children.get (4);
	}
	@Override public Object getValue () {
	    return test + " ? " + thenPart + " : " + elsePart;
	}
    }

    private ParseTreeNode oneOrTwoParter (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return children.get (0);
	return new TwoPartExpression (path, rule, n, children);
    }

    private class TwoPartExpression extends ComplexTreeNode {
	private ParseTreeNode part1;
	private TokenNode operator;
	private ParseTreeNode part2;
	public TwoPartExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    part1 = children.get (0);
	    operator = (TokenNode)children.get (1);
	    part2 = children.get (2);
	}

	@Override public Object getValue() {
	    return part1 + " " + operator + " " + part2;
	}
    }

    private ParseTreeNode unaryExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 1)
	    return children.get (0);
	return new UnaryExpression (path, rule, n, children);
    }

    private class UnaryExpression extends ComplexTreeNode {
	private Token operator;
	private ParseTreeNode exp;
	public UnaryExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    operator = ((TokenNode)children.get (0)).getToken ();
	    exp = children.get (1);
	}

	@Override public Object getValue() {
	    return operator.toString () + exp;
	}
    }

    private class PreIncrementExpression extends ComplexTreeNode {
	private ParseTreeNode expression;
	public PreIncrementExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    expression = children.get (1);
	}

	@Override public Object getValue() {
	    return expression + "++";
	}
    }

    private class PreDecrementExpression extends ComplexTreeNode {
	private ParseTreeNode expression;
	public PreDecrementExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    expression = children.get (1);
	}

	@Override public Object getValue() {
	    return expression + "--";
	}
    }

    private class PostIncrementExpression extends ComplexTreeNode {
	private ParseTreeNode expression;
	public PostIncrementExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    expression = children.get (0);
	}

	@Override public Object getValue() {
	    return expression + "++";
	}
    }

    private class PostDecrementExpression extends ComplexTreeNode {
	private ParseTreeNode expression;
	public PostDecrementExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    expression = children.get (0);
	}

	@Override public Object getValue() {
	    return expression + "--";
	}
    }

    private class CastExpression extends ComplexTreeNode {
	private ParseTreeNode baseType;
	private List<ClassType> additionalBounds;
	private ParseTreeNode expression;

	public CastExpression (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    // starts with (
	    int i = 1;
	    baseType = children.get (i++);
	    if (children.get (i) instanceof ZOMEntry) {
		additionalBounds = new ArrayList<> ();
		ZOMEntry z = (ZOMEntry)children.get (i++);
		z.nodes.forEach (c -> additionalBounds.add ((ClassType)c));
	    } else {
		additionalBounds = Collections.emptyList ();
	    }
	    i++; // ')'
	    expression = children.get (i);
	}

	@Override public Object getValue() {
	    StringBuilder sb = new StringBuilder ();
	    sb.append ("(").append (baseType);
	    if (!additionalBounds.isEmpty ())
		additionalBounds.forEach (a -> sb.append (" & ").append (a));
	    sb.append (")").append (expression);
	    return sb.toString ();
	}
    }

    private ParseTreeNode liftUp (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	return children.get (0);
    }

    private ParseTreeNode buildZom (Rule rule, ParseTreeNode node, List<ParseTreeNode> children) {
	if (rule.size () > 1 && rule.get (0) == rule.getGroupId ()) {
	    ZOMEntry z = (ZOMEntry)children.get (0);
	    z.nodes.addAll (children.subList (1, children.size ()));
	    return z;
	} else {
	    return new ZOMEntry (node.getPosition (), rule.get (0), rule.getName (),
				 new ArrayList<> (children));
	}
    }

    private abstract class CommaListBase extends ComplexTreeNode {
	private final List<ParseTreeNode> params;

	public CommaListBase (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    params = new ArrayList<> ();
	    params.add (children.get (0));
	    if (rule.size () > 1) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int i = 1; i < z.nodes.size (); i += 2)
		    params.add (z.nodes.get (i));
	    }
	}

	public List<ParseTreeNode> get () {
	    return params;
	}

	@Override public Object getValue () {
	    return params.toString ();
	}
    }

    private class ZOMEntry extends ComplexTreeNode {
	private final int ruleGroupId; // zero or more of this group
	private final String name;
	private final List<ParseTreeNode> nodes;

	public ZOMEntry (ParsePosition pos, int ruleGroupId, String name, List<ParseTreeNode> nodes) {
	    super (pos);
	    this.ruleGroupId = ruleGroupId;
	    this.name = name;
	    this.nodes = nodes;
	}

	public int getInternalGroupId () {
	    return ruleGroupId;
	}

	@Override public Object getValue () {
	    return name;
	}

	@SuppressWarnings("unchecked") public <T extends ParseTreeNode> List<T> get () {
	    return (List<T>)nodes;
	}

	@Override public List<ParseTreeNode> getChildren () {
	    return nodes;
	}
    }

    private abstract class ComplexTreeNode implements ParseTreeNode {
	private final ParsePosition pos;

	public ComplexTreeNode (ParsePosition pos) {
	    this.pos = pos;
	}

	@Override public Object getId () {
	    return getClass ().getSimpleName ();
	}

	@Override public boolean isRule () {
	    return true;
	}

	@Override public boolean isToken () {
	    return false;
	}

	@Override public List<ParseTreeNode> getChildren () {
	    return Collections.emptyList ();
	}

	@Override public ParsePosition getPosition () {
	    return pos;
	}

	@Override public String toString () {
	    return getValue ().toString ();
	}
    }

    private static String dotted (Collection<?> c) {
	return c.stream ().map (x -> x.toString ()).collect (Collectors.joining ("."));
    }
}