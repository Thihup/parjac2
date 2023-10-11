package org.khelekore.parjac2.javacompiler.syntaxtree;

import java.util.ArrayList;
import java.util.List;

public class UnannClassType extends ClassType {

    public UnannClassType (SimpleClassType sct) {
	super (sct.getPosition (), new ArrayList<> (List.of (sct)));
    }
}
