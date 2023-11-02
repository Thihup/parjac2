package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.FieldInfo;
import org.khelekore.parjac2.javacompiler.MethodInfo;
import org.khelekore.parjac2.parser.ParsePosition;

public abstract class TypeDeclaration extends FlaggedBase {
    protected TypeDeclaration outerClass;
    protected String localName;
    protected Map<String, List<MethodInfo>> methodInfos;

    public TypeDeclaration (ParsePosition pos) {
	super (pos);
    }

    /** Get the class/enum/interface/annotation name of this type */
    public abstract String getName ();

    /** Get the generic types of this type */
    public abstract TypeParameters getTypeParameters ();

    public abstract ClassType getSuperClass ();

    /** Get all the inner classes, enums, interfaces and annotations */
    public abstract List<TypeDeclaration> getInnerClasses ();

    public void setOuterClass (TypeDeclaration outerClass) {
	this.outerClass = outerClass;
    }

    public TypeDeclaration getOuterClass () {
	return outerClass;
    }

    /** Check if the given type is a local class for this class */
    public abstract boolean isLocalClass (TypeDeclaration td);

    public boolean isLocalClass () {
	return localName != null;
    }

    public void setLocalName (String localName) {
	this.localName = localName;
    }

    public String getLocalName () {
	return localName;
    }

    /**
     * @return a Map from field name to FieldInfo, fields are in declaration order.
     */
    public abstract Map<String, FieldInfo> getFields ();

    public void addField (FieldInfo fi) {
	getFields ().put (fi.name (), fi);
    }

    public abstract List<? extends MethodDeclarationBase> getMethods ();

    public List<MethodInfo> getMethodInformation (String methodName) {
	synchronized (this) {
	    if (methodInfos == null) {
		methodInfos = new HashMap<> ();
		for (MethodDeclarationBase md : getMethods ()) {
		    String name = md.name ();
		    List<MethodInfo> ls = methodInfos.computeIfAbsent (name, n -> new ArrayList<> ());
		    ls.add (md);
		}
	    }
	}
	List<MethodInfo> ret = methodInfos.get (methodName);
	return ret == null ? List.of () : ret;
    }

    public abstract List<? extends ConstructorDeclarationBase> getConstructors ();

    public abstract List<SyntaxTreeNode> getInstanceInitializers ();

    public abstract List<StaticInitializer> getStaticInitializers ();
}
