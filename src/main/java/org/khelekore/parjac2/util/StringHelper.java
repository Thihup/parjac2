package org.khelekore.parjac2.util;

import java.util.Collection;
import java.util.stream.Collectors;

public class StringHelper {

    public static String dotted (Collection<?> c) {
	return c.stream ().map (x -> x.toString ()).collect (Collectors.joining ("."));
    }
}
