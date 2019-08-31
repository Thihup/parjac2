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
*/
	register ("MethodModifier", this::liftUp);
/*
MethodHeader:
*/
	register ("Result", this::liftUp);
/*
MethodDeclarator:
ReceiverParameter:
FormalParameterList:
FormalParameter:
VariableArityParameter:
*/
	register ("VariableModifier", this::liftUp);
/*
Throws:
ExceptionTypeList:
*/
	register ("ExceptionType", this::liftUp);
	register ("MethodBody", this::liftUp);
	register ("InstanceInitializer", this::liftUp);
/*
StaticInitializer:
ConstructorDeclaration:
*/
	register ("ConstructorModifier", this::liftUp);
/*
ConstructorDeclarator:
*/
	register ("SimpleTypeName", this::liftUp);
/*
ConstructorBody:
ExplicitConstructorInvocation:
EnumDeclaration:
EnumBody:
EnumConstantList:
EnumConstant:
*/
	register ("EnumConstantModifier", this::liftUp);
/*
EnumBodyDeclarations:
*/

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
/*
DefaultValue:
*/
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
/*
ArrayInitializer:
VariableInitializerList:
*/
	// Productions from §14 (Blocks and Statements)
/*
Block:
BlockStatements:
*/
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
/*
LabeledStatement:
LabeledStatementNoShortIf:
ExpressionStatement:
*/
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
ArgumentList:
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
/*
Assignment:
*/
	register ("LeftHandSide", this::liftUp);
	register ("AssignmentOperator", this::liftUp);
/*
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
*/
	register ("PostfixExpression", this::liftUp);
/*
PostIncrementExpression:
PostDecrementExpression:
CastExpression:
 */
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
	    return annotations + (annotations.isEmpty () ? "" : " ") + "package " + dotted (nameParts);
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
	    id = ((Identifier)children.get (i++)).getValue ();
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

	@SuppressWarnings("unchecked") @Override public <T extends ParseTreeNode> List<T> getChildren () {
	    return (List<T>)nodes;
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

	@Override public <T extends ParseTreeNode> List<T> getChildren () {
	    return Collections.<T>emptyList ();
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