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
import org.khelekore.parjac2.SourceDiagnostics;
import org.khelekore.parjac2.parser.ParsePosition;
import org.khelekore.parjac2.parser.Rule;
import org.khelekore.parjac2.parser.Token;
import org.khelekore.parjac2.parsetree.ParseTreeNode;
import org.khelekore.parjac2.parsetree.RuleNode;
import org.khelekore.parjac2.parsetree.TokenNode;

public class SyntaxTreeBuilder {
    private final Java11Tokens java11Tokens;
    private final CompilerDiagnosticCollector diagnostics;
    private final Map<String, NodeBuilder> nodeBuilders;

    public SyntaxTreeBuilder (Java11Tokens java11Tokens, CompilerDiagnosticCollector diagnostics) {
	this.java11Tokens = java11Tokens;
	this.diagnostics = diagnostics;
	nodeBuilders = new HashMap<> ();

	register ("GOAL", this::liftUp);

	// Productions from §3 (Lexical Structure)
	register ("TypeIdentifier", this::typeIdentifier);

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
/*
OrdinaryCompilationUnit:
ModularCompilationUnit:
*/
	register ("PackageDeclaration", PackageDeclaration::new);
	register ("PackageModifier", this::liftUp);
	register ("ImportDeclaration", this::liftUp);
	register ("SingleTypeImportDeclaration", SingleTypeImportDeclaration::new);
	register ("TypeImportOnDemandDeclaration", TypeImportOnDemandDeclaration::new);
	register ("SingleStaticImportDeclaration", SingleStaticImportDeclaration::new);
	register ("StaticImportOnDemandDeclaration", StaticImportOnDemandDeclaration::new);
	register ("TypeDeclaration", this::liftUp);
/*
ModuleDeclaration:
ModuleDirective:
RequiresModifier:
*/
	// Productions from §8 (Classes)
	register ("ClassDeclaration", this::liftUp);
/*
NormalClassDeclaration:
*/
	register ("ClassModifier", this::liftUp);
	register ("TypeParameters", TypeParameters::new);
	register ("TypeParameterList", TypeParameterList::new);
	register ("Superclass", Superclass::new);
	register ("Superinterfaces", Superinterfaces::new);
	register ("InterfaceTypeList", InterfaceTypeList::new);
/*
ClassBody:
*/
	register ("ClassBodyDeclaration", this::liftUp);
	register ("ClassMemberDeclaration", this::liftUp);
/*
FieldDeclaration:
FieldModifier:
VariableDeclaratorList:
VariableDeclarator:
VariableDeclaratorId:
*/
	register ("VariableInitializer", this::liftUp);
	register ("UnannType", this::liftUp);
	register ("UnannPrimitiveType", this::liftUp);
	register ("UnannReferenceType", this::liftUp);
	register ("UnannClassType", this::unannClassType);
/*
UnannArrayType:
MethodDeclaration:
MethodModifier:
MethodHeader:
*/
	register ("Result", this::liftUp);
/*
MethodDeclarator:
ReceiverParameter:
FormalParameterList:
FormalParameter:
VariableArityParameter:
VariableModifier:
Throws:
ExceptionTypeList:
ExceptionType:
MethodBody:
InstanceInitializer:
StaticInitializer:
ConstructorDeclaration:
ConstructorModifier:
ConstructorDeclarator:
SimpleTypeName:
ConstructorBody:
ExplicitConstructorInvocation:
EnumDeclaration:
EnumBody:
EnumConstantList:
EnumConstant:
EnumConstantModifier:
EnumBodyDeclarations:
*/

	// Productions from §9 (Interfaces)
/*
InterfaceDeclaration:
NormalInterfaceDeclaration:
InterfaceModifier:
ExtendsInterfaces:
InterfaceBody:
InterfaceMemberDeclaration:
ConstantDeclaration:
ConstantModifier:
InterfaceMethodDeclaration:
InterfaceMethodModifier:
AnnotationTypeDeclaration:
AnnotationTypeBody:
AnnotationTypeMemberDeclaration:
AnnotationTypeElementDeclaration:
AnnotationTypeElementModifier:
DefaultValue:
*/
	register ("Annotation", this::liftUp);
	register ("NormalAnnotation", NormalAnnotation::new);
/*
ElementValuePairList:
ElementValuePair:
ElementValue:
ElementValueArrayInitializer:
ElementValueList:
*/
	register ("MarkerAnnotation", MarkerAnnotation::new);
/*
SingleElementAnnotation:
*/
	// Productions from §10 (Arrays)
/*
ArrayInitializer:
VariableInitializerList:
*/
	// Productions from §14 (Blocks and Statements)
/*
Block:
BlockStatements:
BlockStatement:
LocalVariableDeclarationStatement:
LocalVariableDeclaration:
LocalVariableType:
Statement:
StatementNoShortIf:
StatementWithoutTrailingSubstatement:
EmptyStatement:
LabeledStatement:
LabeledStatementNoShortIf:
ExpressionStatement:
StatementExpression:
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
ForStatement:
ForStatementNoShortIf:
BasicForStatement:
BasicForStatementNoShortIf:
ForInit:
ForUpdate:
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
/*
Primary:
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
ArgumentList:
MethodReference:
ArrayCreationExpression:
DimExprs:
DimExpr:
Expression:
LambdaExpression:
LambdaParameters:
LambdaParameterList:
LambdaParameter:
LambdaParameterType:
LambdaBody:
AssignmentExpression:
Assignment:
LeftHandSide:
AssignmentOperator:
ConditionalExpression:
ConditionalOrExpression:
ConditionalAndExpression:
InclusiveOrExpression:
ExclusiveOrExpression:
AndExpression:
EqualityExpression:
RelationalExpression:
ShiftExpression:
ShiftOp:
AdditiveExpression:
MultiplicativeExpression:
UnaryExpression:
PreIncrementExpression:
PreDecrementExpression:
UnaryExpressionNotPlusMinus:
PostfixExpression:
PostIncrementExpression:
PostDecrementExpression:
CastExpression:
ConstantExpression:
 */
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

    private ParseTreeNode typeIdentifier (Path path, Rule rule, ParseTreeNode input, List<ParseTreeNode> children) {
	Identifier i = (Identifier)children.get (0);
	String name = i.getValue ();
	if (name.equals ("var")) {
	    ParsePosition pp = i.getPosition ();
	    diagnostics.report (SourceDiagnostics.error (path, pp, "A TypeIdentifier may not be named 'var'"));
	}
	return i;
    }

    private class PrimitiveType extends ComplexTreeNode {
	private List<ParseTreeNode> annotations;
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
	private final List<ParseTreeNode> annotations;
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

	public SimpleClassType (ParsePosition pos, List<ParseTreeNode> annotations, String id, TypeArguments tas) {
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
	private List<ParseTreeNode> annotations;
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
	private List<ParseTreeNode> annotations;
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
		// additionalBound below removes the '&' so grab every one of them
		for (int j = 0; j < z.nodes.size (); j++)
		    additionalBounds.add ((ClassType)z.nodes.get (j));
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

    private class TypeArgumentList extends ComplexTreeNode {
	private final List<ParseTreeNode> ls;

	public TypeArgumentList (Path path, Rule r, ParseTreeNode tal, List<ParseTreeNode> children) {
	    super (tal.getPosition ());
	    ParseTreeNode ta = children.get (0);
	    if (r.size () == 1) {
		ls = Collections.singletonList (ta);
	    } else {
		ZOMEntry ze = (ZOMEntry)children.get (1);
		ls = ze.get ();
		ls.add (0, ta);
	    }
	}

	public List<ParseTreeNode> get () {
	    return ls;
	}

	@Override public Object getValue () {
	    return ls.toString ();
	}
    }

    private class Wildcard extends ComplexTreeNode {
	private final List<ParseTreeNode> annotations;
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

    private class PackageDeclaration extends ComplexTreeNode {
	private final List<ParseTreeNode> annotations;
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
	    return annotations + (annotations.isEmpty () ? "" : " ") + "package " + dotted (nameParts);
	}
    }

    private class SingleTypeImportDeclaration extends ComplexTreeNode {
	private TypeName typename;
	public SingleTypeImportDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (1);
	}

	@Override public Object getValue () {
	    return "import " + typename + ";";
	}
    }

    private class TypeImportOnDemandDeclaration extends ComplexTreeNode {
	private final PackageOrTypeName typename;
	public TypeImportOnDemandDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (PackageOrTypeName)children.get (1);
	}

	@Override public Object getValue () {
	    return "import " + typename + ".*;";
	}
    }

    private class SingleStaticImportDeclaration extends ComplexTreeNode {
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

    private class StaticImportOnDemandDeclaration extends ComplexTreeNode {
	private TypeName typename;
	public StaticImportOnDemandDeclaration (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (2);
	}

	@Override public Object getValue () {
	    return "import static " + typename + ".*;";
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

    private class TypeParameterList extends ComplexTreeNode {
	private final List<TypeParameter> params;

	public TypeParameterList (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    params = new ArrayList<> ();
	    params.add ((TypeParameter)children.get (0));
	    if (rule.size () > 1) {
		ZOMEntry z = (ZOMEntry)children.get (1);
		for (int i = 1; i < z.nodes.size (); i += 2)
		    params.add ((TypeParameter)z.nodes.get (i));
	    }
	}

	@Override public Object getValue () {
	    return params.toString ();
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

    private ParseTreeNode unannClassType (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	if (rule.size () == 3) {
	    UnannClassType uct = (UnannClassType)children.get (0);
	    SimpleClassType sct = (SimpleClassType)children.get (2);
	    uct.types.add (sct);
	    return uct;
	} else {
	    Identifier i = (Identifier)children.get (0);
	    TypeArguments tas = children.size () > 1 ? (TypeArguments)children.get (1) : null;
	    SimpleClassType sct = new SimpleClassType (n.getPosition (), Collections.emptyList (), i.getValue (), tas);
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

    private class NormalAnnotation extends ComplexTreeNode {
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

    private class MarkerAnnotation extends ComplexTreeNode {
	private TypeName typename;
	public MarkerAnnotation (Path path, Rule rule, ParseTreeNode n, List<ParseTreeNode> children) {
	    super (n.getPosition ());
	    this.typename = (TypeName)children.get (1);
	}

	@Override public Object getValue() {
	    return "@" + typename;
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
	    return new ZOMEntry (node.getPosition (), rule.getName (), new ArrayList<> (children));
	}
    }

    private class ZOMEntry extends ComplexTreeNode {
	private final String name;
	private final List<ParseTreeNode> nodes;

	public ZOMEntry (ParsePosition pos, String name, List<ParseTreeNode> nodes) {
	    super (pos);
	    this.name = name;
	    this.nodes = nodes;
	}

	@Override public Object getValue () {
	    return name;
	}

	public List<ParseTreeNode> get () {
	    return nodes;
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