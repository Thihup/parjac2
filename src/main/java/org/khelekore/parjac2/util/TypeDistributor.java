package org.khelekore.parjac2.util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class TypeDistributor {
    private Map<Class<?>, Consumer<?>> mappings;

    public TypeDistributor () {
	mappings = new HashMap<> ();
    }

    @SuppressWarnings("unchecked")
    public <T> void addMapping (Class<?> c, List<T> ls) {
	if (ls == null)
	    throw new NullPointerException ("List may not be null");
	addMapping (c, t -> ls.add ((T)t));
    }

    public <T> void skip (Class<T> c) {
	addMapping (c, t -> { /* nothing */ });
    }

    private <T> void addMapping (Class<?> cls, Consumer<T> c) {
	mappings.put (cls, c);
    }

    @SuppressWarnings("unchecked")
    public <T> void distribute (T t) {
	Consumer<T> c = (Consumer<T>)mappings.get (t.getClass ());
	if (c == null)
	    throw new IllegalStateException ("Unhandled type: " + t.getClass () + ": " + t);
	c.accept (t);
    }
}