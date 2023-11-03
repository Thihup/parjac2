package org.khelekore.parjac2.javacompiler;

import java.util.Iterator;
import java.util.List;

import org.khelekore.parjac2.javacompiler.syntaxtree.FullNameHandler;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeDeclaration;
import org.khelekore.parjac2.javacompiler.syntaxtree.TypeParameter;

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
