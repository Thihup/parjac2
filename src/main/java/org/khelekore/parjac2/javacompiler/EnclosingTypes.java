package org.khelekore.parjac2.javacompiler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.LocalVariableDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;
import org.khelekore.parjac2.javacompiler.syntaxtree.VariableDeclarator;

public record EnclosingTypes (EnclosingTypes previous, Enclosure<?> enclosure)
    implements Iterable<EnclosingTypes> {

    @Override public Iterator<EnclosingTypes> iterator () {
	return new ETIterator (this);
    }

    public boolean isStatic () {
	return enclosure.isStatic ();
    }

    public TypeDeclaration td () {
	return enclosure.td ();
    }

    public FullNameHandler fqn () {
	return enclosure.fqn ();
    }

    public List<FullNameHandler> getSuperClasses (ClassInformationProvider cip) {
	return enclosure.getSuperClasses (cip);
    }

    public TypeParameter getTypeParameter (String id) {
	return enclosure.getTypeParameter (id);
    }

    public static EnclosingTypes topLevel (TypeDeclaration td, ClassInformationProvider cip) {
	return new EnclosingTypes (null, new TypeEnclosure (td, cip.getFullName (td)));
    }

    public EnclosingTypes enclosingTypes (TypeDeclaration td, ClassInformationProvider cip) {
	return new EnclosingTypes (this, new TypeEnclosure (td, cip.getFullName (td)));
    }

    public EnclosingTypes enclosingTypeParameter (Map<String, TypeParameter> nameToTypeParameter) {
	return new EnclosingTypes (this, new TypeParameterEnclosure (nameToTypeParameter));
    }

    public EnclosingTypes enclosingVariables (Map<String, VariableInfo> nameToVariable, boolean isStatic) {
	return new EnclosingTypes (this, new VariableEnclosure (nameToVariable, isStatic));
    }

    public EnclosingTypes enclosingBlock (boolean staticContext) {
	return new EnclosingTypes (this, new BlockEnclosure (staticContext));
    }

    public record TypeEnclosure (TypeDeclaration td, FullNameHandler fqn) implements Enclosure<FieldInfo> {
	@Override public boolean isStatic () { return Flags.isStatic (td.flags ()); }
	@Override public TypeDeclaration td () { return td; }
	@Override public FullNameHandler fqn () { return fqn; }
	@Override public List<FullNameHandler> getSuperClasses (ClassInformationProvider cip) {
	    try {
		return cip.getSuperTypes (fqn.getFullDotName (), false);
	    } catch (IOException e) {
		throw new RuntimeException ("Unable to load superclasses of: " + td.getName ());
	    }
	}
	@Override public Map<String, FieldInfo> getFields () { return td.getFields (); }
    }

    private record TypeParameterEnclosure (Map<String, TypeParameter> nameToTypeParameter) implements Enclosure<VariableInfo> {
	@Override public boolean isStatic () { return false; }
	@Override public TypeParameter getTypeParameter (String id) { return nameToTypeParameter.get (id); }
	@Override public Map<String, VariableInfo> getFields () { return Map.of (); }
    }

    private record VariableEnclosure (Map<String, VariableInfo> variables, boolean isStatic) implements Enclosure<VariableInfo> {
	@Override public Map<String, VariableInfo> getFields () { return variables; }
    }

    public static class BlockEnclosure implements Enclosure<VariableInfo> {
	private final boolean isStatic;
	private Map<String, VariableInfo> locals = Map.of ();

	public BlockEnclosure (boolean isStatic) {
	    this.isStatic = isStatic;
	}

	@Override public boolean isStatic () {
	    return isStatic;
	}

	public void add (LocalVariableDeclaration lv) {
	    if (locals.isEmpty ())
		locals = new HashMap<> ();
	    for (VariableDeclarator vd : lv.getDeclarators ()) {
		String name = vd.getName ();
		VariableInfo vi = new FieldInfo (name, vd.position (), Flags.ACC_PUBLIC, lv.getType (), vd.rank ());
		locals.put (name, vi);
	    }
	}

	@Override public Map<String, VariableInfo> getFields () {
	    return locals;
	}
    }

    private static class ETIterator implements Iterator<EnclosingTypes> {
	private EnclosingTypes e;

	public ETIterator (EnclosingTypes e) {
	    this.e = e;
	}

	@Override public boolean hasNext () {
	    return e != null;
	}

	@Override public EnclosingTypes next () {
	    EnclosingTypes ret = e;
	    e = e.previous ();
	    return ret;
	}
    }
}
