package org.khelekore.parjac2.javacompiler;

import java.util.HashMap;
import java.util.Map;

/** A ClassLoader that uses the given byte arrays as classes. */
public class InMemoryClassLoader extends ClassLoader {
    private final Map<String, byte[]> classes;

    public InMemoryClassLoader (Map<String, byte[]> classes) {
	this.classes = classes;
    }

    @Override protected Class<?> findClass (String name) throws ClassNotFoundException {
	byte[] data = classes.get (name);
	if (data == null)
	    throw new ClassNotFoundException (name + " not found");
	return defineClass (name, data, 0, data.length);
    }

    public Map<String, Class<?>> loadAllClasses () throws ClassNotFoundException {
	Map<String, Class<?>> ret = new HashMap<> ();
	for (String name : classes.keySet ())
	    ret.put (name, loadClass (name));
	return ret;
    }
}
