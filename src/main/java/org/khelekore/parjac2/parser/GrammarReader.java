package org.khelekore.parjac2.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class GrammarReader {
    private final Grammar grammar;
    private String currentRule;
    private Map<ZeroOrMore, String> zomToName = new HashMap<> ();

    private static final String LINE_SPLITTER = " \t\f{[]}'";
    private static final String TOKEN_SPLITTER = "'";

    public GrammarReader (Grammar grammar, boolean debug) {
	this.grammar = grammar;
    }

    public void read (URL u) throws IOException {
	try (InputStream is = u.openStream ();
	     InputStreamReader isr = new InputStreamReader (is, "UTF-8")) {
	    read (isr);
	}
    }

    public void read (Reader isr) throws IOException {
	try (BufferedReader br = new BufferedReader (isr)) {
	    String line = null;
	    while ((line = br.readLine ()) != null) {
		parseLine (line);
	    }
	}
    }

    // TODO: rewrite it in parjac :-)
    private void parseLine (String line) {
	int hashIndex = line.indexOf ('#');
	if (hashIndex > -1)
	    line = line.substring (0, hashIndex);
	line = line.trim ();
	if (line.isEmpty ())
	    return;
	if (line.endsWith (":")) {
	    currentRule = line.substring (0, line.length () - 1);
	} else {
	    StringTokenizer st = new StringTokenizer (line, LINE_SPLITTER, true);
	    addRules (currentRule, parseRecursive (st));
	}
    }

    private Object parseRecursive (StringTokenizer st) {
	List<Object> currentSequence = new ArrayList<> ();
	Object lastParsed = null;
	while (st.hasMoreElements ()) {
	    String s = st.nextToken ();
	    switch (s) {
	    case " ": // split on blanks
		if (lastParsed != null)
		    currentSequence.add (lastParsed);
		lastParsed = null;
		break;
	    case "{":
	    case "[":
		lastParsed = parseRecursive (st);
		break;
	    case "]":
		return new ZeroOrOne (finish (currentSequence, lastParsed));
	    case "}":
		return new ZeroOrMore (finish (currentSequence, lastParsed));
	    case "'":
		lastParsed = grammar.getToken (st.nextToken (TOKEN_SPLITTER));
		st.nextToken (LINE_SPLITTER);     // skip final '
		break;
	    default:
		lastParsed = s; // rule name
		break;
	    }
	}
	return finish (currentSequence, lastParsed);
    }

    private Object finish (List<Object> currentSequence, Object lastParsed) {
	currentSequence.add (lastParsed);
	return sequence (currentSequence);
    }

    public Object sequence (List<Object> os) {
	if (os.size () == 1)
	    return os.get (0);
	return os;
    }

    private void addRules (String name, Object parts) {
	List<List<Object>> finalParts = new ArrayList<> ();

	finalParts.add (new ArrayList<> ());
	split (parts, finalParts);
	for (List<Object> ls : finalParts) {
	    if (ls.isEmpty ())
		continue;
	    grammar.addRule (name, toInts (ls));
	}
    }

    /** Split the complex rules into simple rules.
     *  Each simple rule only consists of tokens and rule names
     * @param parts the resulting simple rules
     */
    private void split (Object p, List<List<Object>> parts) {
	if (p instanceof Token || p instanceof String) {
	    for (List<Object> ls : parts)
		ls.add (p);
	} else if (p instanceof ZeroOrOne) {
	    ZeroOrOne zoo = (ZeroOrOne)p;
	    int s = parts.size ();
	    List<List<Object>> newParts = new ArrayList<> ();
	    for (int i = 0; i < s; i++)
		newParts.add (new ArrayList<> (parts.get (i)));
	    split (zoo.data, newParts);
	    parts.addAll (newParts);
	} else if (p instanceof ZeroOrMore) {
	    ZeroOrMore zom = (ZeroOrMore)p;
	    int s = parts.size ();
	    for (int i = 0; i < s; i++)
		parts.add (new ArrayList<> (parts.get (i)));
	    String zomName = zomToName.computeIfAbsent (zom, this::addZomRule);
	    for (int i = s; i < parts.size (); i++)
		parts.get (i).add (zomName);
	} else if (p instanceof List) {
	    List<?> ls = (List<?>)p;
	    for (Object x : ls)
		split (x, parts);
	} else {
	    throw new IllegalStateException ("Unhandled type: " + p);
	}
    }

    private String addZomRule (ZeroOrMore zom) {
	String name = "_ZOM" + zomToName.size () + "{" + zom.data + "}";
	addRules (name, List.of (new ZeroOrOne (name), zom.data));
	return name;
    }

    private int[] toInts (List<Object> ls) {
	int[] ret = new int[ls.size ()];
	for (int i = 0; i < ls.size (); i++) {
	    int v;
	    Object o = ls.get (i);
	    if (o instanceof Token)
		v = ((Token)o).getId ();
	    else
		v = grammar.getRuleGroupId ((String)o);
	    ret[i] = v;
	}
	return ret;
    }

    private static class ZeroOrOne {
	private final Object data;
	public ZeroOrOne (Object data) {
	    this.data = data;
	}

	@Override public String toString () {
	    return "[" + data + "]";
	}
    }

    private static class ZeroOrMore {
	private final Object data;
	public ZeroOrMore (Object data) {
	    this.data = data;
	}

	@Override public String toString () {
	    return "{" + data + "}";
	}

	@Override public int hashCode () {
	    return data.hashCode ();
	}

	@Override public boolean equals (Object o) {
	    if (o == this)
		return true;
	    if (o == null)
		return false;
	    if (o.getClass () != getClass ())
		return false;
	    return data.equals (((ZeroOrMore)o).data);
	}
    }
}
